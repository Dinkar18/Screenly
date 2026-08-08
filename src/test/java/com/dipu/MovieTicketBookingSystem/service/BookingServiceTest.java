package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.BookingRequest;
import com.dipu.MovieTicketBookingSystem.dto.BookingResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.*;
import com.dipu.MovieTicketBookingSystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private com.dipu.MovieTicketBookingSystem.security.JwtUtil jwtUtil;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Showtime testShowtime;
    private ShowtimeSeat testSeat;
    private UUID showtimeId;
    private UUID seatId;

    @BeforeEach
    void setUp() {
        showtimeId = UUID.randomUUID();
        seatId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@cinereserve.com");

        Seat actualSeat = new Seat();
        actualSeat.setId(UUID.randomUUID());
        actualSeat.setSeatIdentifier("A1");

        testShowtime = new Showtime();
        testShowtime.setId(showtimeId);
        testShowtime.setPrice(BigDecimal.valueOf(150.00));
        
        Movie testMovie = new Movie();
        testMovie.setTitle("Test Movie");
        testShowtime.setMovie(testMovie);
        
        Screen testScreen = new Screen();
        testScreen.setName("Test Screen");
        Theater testTheater = new Theater();
        testTheater.setName("Test Theater");
        testScreen.setTheater(testTheater);
        testShowtime.setScreen(testScreen);

        testSeat = new ShowtimeSeat();
        testSeat.setId(seatId);
        testSeat.setShowtime(testShowtime);
        testSeat.setSeat(actualSeat);
        testSeat.setStatus(com.dipu.MovieTicketBookingSystem.model.enums.SeatStatus.AVAILABLE);
    }

    @Test
    void createBooking_Success() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtimeId);
        request.setShowtimeSeatIds(List.of(seatId));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(testShowtime));
        // Simulate pessimistic lock
        when(showtimeSeatRepository.findByIdsForUpdate(request.getShowtimeSeatIds())).thenReturn(List.of(testSeat));
        
        Booking savedBooking = new Booking();
        savedBooking.setId(UUID.randomUUID());
        savedBooking.setUser(testUser);
        savedBooking.setShowtime(testShowtime);
        savedBooking.setTotalAmount(BigDecimal.valueOf(150.00));
        savedBooking.setStatus(com.dipu.MovieTicketBookingSystem.model.enums.BookingStatus.CONFIRMED);
        savedBooking.setCreatedAt(java.time.LocalDateTime.now());
        
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // Act
        BookingResponse response = bookingService.createBooking(request, "user@cinereserve.com", "test-idempotency-key");

        // Assert
        assertNotNull(response);
        assertEquals(com.dipu.MovieTicketBookingSystem.model.enums.SeatStatus.RESERVED, testSeat.getStatus());
        verify(showtimeSeatRepository, times(1)).findByIdsForUpdate(request.getShowtimeSeatIds());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_SeatAlreadyBooked_ThrowsException() {
        // Arrange
        BookingRequest request = new BookingRequest();
        request.setShowtimeId(showtimeId);
        request.setShowtimeSeatIds(List.of(seatId));

        testSeat.setStatus(com.dipu.MovieTicketBookingSystem.model.enums.SeatStatus.RESERVED);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(testShowtime));
        when(showtimeSeatRepository.findByIdsForUpdate(request.getShowtimeSeatIds())).thenReturn(List.of(testSeat));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(request, "user@cinereserve.com", "test-idempotency-key");
        });

        assertTrue(exception.getMessage().contains("is already booked"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }
}
