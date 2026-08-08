package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.ShowtimeRequest;
import com.dipu.MovieTicketBookingSystem.dto.ShowtimeResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Movie;
import com.dipu.MovieTicketBookingSystem.model.entity.Screen;
import com.dipu.MovieTicketBookingSystem.model.entity.Seat;
import com.dipu.MovieTicketBookingSystem.model.entity.Showtime;
import com.dipu.MovieTicketBookingSystem.model.entity.Theater;
import com.dipu.MovieTicketBookingSystem.repository.MovieRepository;
import com.dipu.MovieTicketBookingSystem.repository.ScreenRepository;
import com.dipu.MovieTicketBookingSystem.repository.SeatRepository;
import com.dipu.MovieTicketBookingSystem.repository.ShowtimeRepository;
import com.dipu.MovieTicketBookingSystem.repository.ShowtimeSeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShowtimeServiceTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ScreenRepository screenRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;

    @InjectMocks
    private ShowtimeService showtimeService;

    private Movie testMovie;
    private Screen testScreen;
    private Theater testTheater;
    private UUID movieId;
    private UUID screenId;
    private UUID showtimeId;

    @BeforeEach
    void setUp() {
        movieId = UUID.randomUUID();
        screenId = UUID.randomUUID();
        showtimeId = UUID.randomUUID();

        testMovie = Movie.builder()
                .id(movieId)
                .title("Interstellar")
                .durationMinutes(169)
                .build();

        testTheater = Theater.builder()
                .id(UUID.randomUUID())
                .name("IMAX Theater")
                .build();

        testScreen = Screen.builder()
                .id(screenId)
                .name("Screen 1")
                .theater(testTheater)
                .build();
    }

    @Test
    void createShowtime_Success() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        
        ShowtimeRequest request = new ShowtimeRequest();
        request.setMovieId(movieId);
        request.setScreenId(screenId);
        request.setStartTime(startTime);
        request.setPrice(BigDecimal.valueOf(250.00));

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        when(screenRepository.findById(screenId)).thenReturn(Optional.of(testScreen));
        
        // No overlapping showtimes
        when(showtimeRepository.findOverlappingShowtimes(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        Showtime savedShowtime = Showtime.builder()
                .id(showtimeId)
                .movie(testMovie)
                .screen(testScreen)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(169).plusMinutes(30))
                .price(BigDecimal.valueOf(250.00))
                .build();

        when(showtimeRepository.save(any(Showtime.class))).thenReturn(savedShowtime);

        // Mock seats
        Seat mockSeat = Seat.builder().id(UUID.randomUUID()).build();
        when(seatRepository.findByScreenId(screenId)).thenReturn(List.of(mockSeat));

        ShowtimeResponse response = showtimeService.createShowtime(request);

        assertNotNull(response);
        assertEquals(showtimeId, response.getId());
        assertEquals("Interstellar", response.getMovieTitle());
        
        verify(showtimeRepository, times(1)).save(any(Showtime.class));
        verify(showtimeSeatRepository, times(1)).saveAll(anyList());
    }

    @Test
    void createShowtime_Overlapping_ThrowsException() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        
        ShowtimeRequest request = new ShowtimeRequest();
        request.setMovieId(movieId);
        request.setScreenId(screenId);
        request.setStartTime(startTime);

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        when(screenRepository.findById(screenId)).thenReturn(Optional.of(testScreen));
        
        Showtime existingShowtime = new Showtime();
        when(showtimeRepository.findOverlappingShowtimes(any(UUID.class), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(existingShowtime));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> showtimeService.createShowtime(request));
        assertEquals("Screen is already booked for this time period", exception.getMessage());
        
        verify(showtimeRepository, never()).save(any(Showtime.class));
        verify(showtimeSeatRepository, never()).saveAll(anyList());
    }

    @Test
    void getShowtimesByMovie_Success() {
        Showtime showtime = Showtime.builder()
                .id(showtimeId)
                .movie(testMovie)
                .screen(testScreen)
                .startTime(LocalDateTime.now().plusDays(1))
                .build();
                
        when(showtimeRepository.findByMovieIdAndStartTimeAfterAndIsActiveTrueOrderByStartTimeAsc(eq(movieId), any(LocalDateTime.class)))
                .thenReturn(List.of(showtime));

        List<ShowtimeResponse> responses = showtimeService.getShowtimesByMovie(movieId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Interstellar", responses.get(0).getMovieTitle());
    }
}
