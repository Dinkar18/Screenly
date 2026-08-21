package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.BookingRequest;
import com.dipu.MovieTicketBookingSystem.dto.BookingResponse;
import com.dipu.MovieTicketBookingSystem.dto.SeatResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Booking;
import com.dipu.MovieTicketBookingSystem.model.entity.Showtime;
import com.dipu.MovieTicketBookingSystem.model.entity.ShowtimeSeat;
import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus;
import com.dipu.MovieTicketBookingSystem.model.enums.SeatStatus;
import com.dipu.MovieTicketBookingSystem.repository.BookingRepository;
import com.dipu.MovieTicketBookingSystem.repository.ShowtimeRepository;
import com.dipu.MovieTicketBookingSystem.repository.ShowtimeSeatRepository;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.exception.SeatUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public List<SeatResponse> getSeatsForShowtime(UUID showtimeId) {
        return showtimeSeatRepository.findByShowtimeId(showtimeId).stream()
                .map(this::mapToShowtimeSeatResponse)
                .sorted(java.util.Comparator.comparingInt(SeatResponse::getRowNumber)
                        .thenComparing(SeatResponse::getSeatLetter))
                .collect(Collectors.toList());
    }

    private SeatResponse mapToShowtimeSeatResponse(ShowtimeSeat s) {
        String identifier = s.getSeat().getSeatIdentifier();
        int rowNumber = 1;
        String seatLetter = "A";

        // Clean standard: Backend extracts business domain fields explicitly
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)([A-Za-z]+)$").matcher(identifier);
        if (matcher.matches()) {
            rowNumber = Integer.parseInt(matcher.group(1));
            seatLetter = matcher.group(2).toUpperCase();
        }

        return SeatResponse.builder()
                .showtimeSeatId(s.getId())
                .seatIdentifier(identifier)
                .rowNumber(rowNumber)
                .seatLetter(seatLetter)
                .status(s.getStatus())
                .build();
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail, String idempotencyKey) {
        // Idempotency is now handled declaratively via AOP Aspect (@Idempotent) in the Controller.

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        // 2. Pessimistic Locking: Lock the requested seats to prevent concurrent double-booking
        List<ShowtimeSeat> requestedSeats = showtimeSeatRepository.findByIdsForUpdate(request.getShowtimeSeatIds());
        
        if (requestedSeats.size() != request.getShowtimeSeatIds().size()) {
            throw new IllegalArgumentException("One or more requested seats are invalid");
        }

        // Verify all requested seats belong to this showtime
        if (requestedSeats.stream().anyMatch(s -> !s.getShowtime().getId().equals(showtime.getId()))) {
            throw new IllegalArgumentException("Seat does not belong to this showtime");
        }

        // 3. Status Verification (If any seat is booked, the whole transaction rolls back)
        for (ShowtimeSeat seat : requestedSeats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                log.warn("Seat {} is no longer available", seat.getSeat().getSeatIdentifier());
                throw new SeatUnavailableException("Seat " + seat.getSeat().getSeatIdentifier() + " is already booked by someone else!");
            }
        }

        // 4. Calculate total price
        BigDecimal totalAmount = showtime.getPrice().multiply(new BigDecimal(requestedSeats.size()));

        // 5. Create Booking
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING) // Set to PENDING initially
                .idempotencyKey(idempotencyKey)
                .build();
        
        Booking savedBooking = bookingRepository.save(booking);

        // 6. Update seats to RESERVED
        for (ShowtimeSeat seat : requestedSeats) {
            seat.setStatus(SeatStatus.RESERVED);
            seat.setBooking(savedBooking);
        }
        showtimeSeatRepository.saveAll(requestedSeats);

        log.info("Successfully reserved {} seats for user {}, pending payment", requestedSeats.size(), userEmail);
        
        return mapToResponse(savedBooking, requestedSeats);
    }

    @Transactional
    public void confirmBooking(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking {} is already confirmed.", bookingId);
            return;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Update seats to BOOKED
        List<ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeId(booking.getShowtime().getId()).stream()
                .filter(s -> booking.getId().equals(s.getBooking() != null ? s.getBooking().getId() : null))
                .collect(Collectors.toList());

        for (ShowtimeSeat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);
        }
        showtimeSeatRepository.saveAll(seats);

        log.info("Payment successful, booking {} confirmed.", bookingId);

        BookingResponse response = mapToResponse(booking, seats);
        // Fire Asynchronous Event (Generate PDF and Email)
        notificationService.sendBookingConfirmation(response, booking.getUser().getEmail());
    }

    public com.dipu.MovieTicketBookingSystem.dto.PageResponse<BookingResponse> getMyBookings(String userEmail, org.springframework.data.domain.Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        org.springframework.data.domain.Page<Booking> bookingPage = bookingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), BookingStatus.CONFIRMED, pageable);
        List<BookingResponse> responses = bookingPage.getContent().stream()
                .map(booking -> {
                    // Fetch seats for this booking
                    List<ShowtimeSeat> bookedSeats = showtimeSeatRepository.findByShowtimeId(booking.getShowtime().getId())
                            .stream()
                            .filter(s -> booking.getId().equals(s.getBooking() != null ? s.getBooking().getId() : null))
                            .collect(Collectors.toList());
                    return mapToResponse(booking, bookedSeats);
                })
                .collect(Collectors.toList());

        return com.dipu.MovieTicketBookingSystem.dto.PageResponse.<BookingResponse>builder()
                .content(responses)
                .pageNumber(bookingPage.getNumber())
                .pageSize(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .isLast(bookingPage.isLast())
                .build();
    }

    private BookingResponse mapToResponse(Booking booking, List<ShowtimeSeat> seats) {
        List<String> seatNumbers = seats.stream()
                .map(s -> s.getSeat().getSeatIdentifier())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .theaterName(booking.getShowtime().getScreen().getTheater().getName())
                .screenName(booking.getShowtime().getScreen().getName())
                .showtime(booking.getShowtime().getStartTime())
                .bookedSeats(seatNumbers)
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
