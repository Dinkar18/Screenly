package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.ScreenRequest;
import com.dipu.MovieTicketBookingSystem.dto.ScreenResponse;
import com.dipu.MovieTicketBookingSystem.dto.TheaterRequest;
import com.dipu.MovieTicketBookingSystem.dto.TheaterResponse;
import com.dipu.MovieTicketBookingSystem.exception.InvalidOperationException;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.model.entity.Screen;
import com.dipu.MovieTicketBookingSystem.model.entity.Seat;
import com.dipu.MovieTicketBookingSystem.model.entity.Showtime;
import com.dipu.MovieTicketBookingSystem.model.entity.Theater;
import com.dipu.MovieTicketBookingSystem.repository.BookingRepository;
import com.dipu.MovieTicketBookingSystem.repository.ScreenRepository;
import com.dipu.MovieTicketBookingSystem.repository.SeatRepository;
import com.dipu.MovieTicketBookingSystem.repository.ShowtimeRepository;
import com.dipu.MovieTicketBookingSystem.repository.TheaterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TheaterService {

    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;

    // --- Theater Operations ---

    public TheaterResponse createTheater(TheaterRequest request) {
        Theater theater = Theater.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .build();
        
        Theater savedTheater = theaterRepository.save(theater);
        return mapToTheaterResponse(savedTheater);
    }

    @Transactional(readOnly = true)
    public List<TheaterResponse> getAllTheaters() {
        return theaterRepository.findByIsActiveTrue().stream()
                .map(this::mapToTheaterResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TheaterResponse getTheaterById(UUID id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));
        return mapToTheaterResponse(theater);
    }

    public TheaterResponse updateTheater(UUID id, TheaterRequest request) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));

        theater.setName(request.getName());
        theater.setCity(request.getCity());
        theater.setAddress(request.getAddress());

        Theater updatedTheater = theaterRepository.save(theater);
        return mapToTheaterResponse(updatedTheater);
    }

    public void deleteTheater(UUID id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));
        
        // 1. Check if customers have already purchased tickets for future shows at this theater
        boolean hasActiveBookings = bookingRepository.existsActiveFutureBookingsByTheater(id, LocalDateTime.now());
        if (hasActiveBookings) {
            throw new InvalidOperationException("Cannot delete theater because customers have active bookings for upcoming shows. Please cancel and refund those bookings first.");
        }

        // 2. Soft-delete the theater
        theater.setActive(false);

        // 3. Soft-delete all screens in this theater
        if (theater.getScreens() != null) {
            theater.getScreens().forEach(s -> s.setActive(false));
        }
        
        // 4. Deactivate all unbooked future showtimes automatically
        List<Showtime> futureShowtimes = showtimeRepository.findFutureShowtimesByTheater(id, LocalDateTime.now());
        if (!futureShowtimes.isEmpty()) {
            futureShowtimes.forEach(st -> st.setActive(false));
            showtimeRepository.saveAll(futureShowtimes);
        }

        theaterRepository.save(theater);
    }

    // --- Screen Operations ---

    public ScreenResponse addScreen(ScreenRequest request) {
        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(() -> new ResourceNotFoundException("Theater not found"));

        // Check if screen name already exists in this theater
        boolean screenExists = theater.getScreens() != null && theater.getScreens().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(request.getName()));
        
        if (screenExists) {
            throw new InvalidOperationException("Screen name already exists in this theater");
        }

        Screen screen = Screen.builder()
                .theater(theater)
                .name(request.getName())
                .capacity(request.getCapacity())
                .build();

        Screen savedScreen = screenRepository.save(screen);

        // Auto-generate physical seats in batch (e.g. 1A, 1B... 10J)
        generateSeatsForScreen(savedScreen);

        return mapToScreenResponse(savedScreen);
    }

    private void generateSeatsForScreen(Screen screen) {
        int capacity = screen.getCapacity();
        int rows = (int) Math.ceil((double) capacity / 10);
        int currentSeatCount = 0;
        List<Seat> seats = new ArrayList<>(capacity);

        for (int row = 0; row < rows; row++) {
            for (int num = 1; num <= 10; num++) {
                if (currentSeatCount >= capacity) break;

                String seatIdentifier = String.valueOf(row + 1) + getExcelColumnName(num);
                Seat seat = Seat.builder()
                        .screen(screen)
                        .seatIdentifier(seatIdentifier)
                        .build();
                seats.add(seat);
                currentSeatCount++;
            }
        }
        seatRepository.saveAll(seats);
    }

    private String getExcelColumnName(int n) {
        StringBuilder result = new StringBuilder();
        while (n > 0) {
            n--;
            result.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return result.toString();
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheaterId(UUID theaterId) {
        // Validate theater exists
        if (!theaterRepository.existsById(theaterId)) {
            throw new ResourceNotFoundException("Theater not found");
        }
        
        return screenRepository.findByTheaterIdAndIsActiveTrue(theaterId).stream()
                .map(this::mapToScreenResponse)
                .collect(Collectors.toList());
    }

    public ScreenResponse updateScreen(UUID id, ScreenRequest request) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        // Name check if changed
        if (!screen.getName().equalsIgnoreCase(request.getName())) {
            boolean screenExists = screen.getTheater().getScreens().stream()
                    .anyMatch(s -> s.getName().equalsIgnoreCase(request.getName()));
            if (screenExists) {
                throw new InvalidOperationException("Screen name already exists in this theater");
            }
        }

        screen.setName(request.getName());
        screen.setCapacity(request.getCapacity());
        
        Screen updatedScreen = screenRepository.save(screen);
        return mapToScreenResponse(updatedScreen);
    }

    public void deleteScreen(UUID id) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
        
        // 1. Check if customers have active bookings for future shows on this screen
        boolean hasActiveBookings = bookingRepository.existsActiveFutureBookingsByScreen(id, LocalDateTime.now());
        if (hasActiveBookings) {
            throw new InvalidOperationException("Cannot delete screen because customers have active bookings for upcoming shows. Please cancel and refund those bookings first.");
        }
        
        // 2. Soft-delete the screen
        screen.setActive(false);
        screenRepository.save(screen);

        // 3. Deactivate all unbooked future showtimes on this screen
        List<Showtime> futureShowtimes = showtimeRepository.findAll().stream()
                .filter(s -> s.getScreen().getId().equals(id) && s.isActive() && s.getStartTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        if (!futureShowtimes.isEmpty()) {
            futureShowtimes.forEach(st -> st.setActive(false));
            showtimeRepository.saveAll(futureShowtimes);
        }
    }

    // --- Mappers ---

    private TheaterResponse mapToTheaterResponse(Theater theater) {
        return TheaterResponse.builder()
                .id(theater.getId())
                .name(theater.getName())
                .city(theater.getCity())
                .address(theater.getAddress())
                .build();
    }

    private ScreenResponse mapToScreenResponse(Screen screen) {
        return ScreenResponse.builder()
                .id(screen.getId())
                .theaterId(screen.getTheater().getId())
                .name(screen.getName())
                .capacity(screen.getCapacity())
                .build();
    }
}
