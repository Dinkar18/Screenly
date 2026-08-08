package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.ShowtimeRequest;
import com.dipu.MovieTicketBookingSystem.dto.ShowtimeResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Movie;
import com.dipu.MovieTicketBookingSystem.model.entity.Screen;
import com.dipu.MovieTicketBookingSystem.model.entity.Showtime;
import com.dipu.MovieTicketBookingSystem.repository.MovieRepository;
import com.dipu.MovieTicketBookingSystem.repository.ScreenRepository;
import com.dipu.MovieTicketBookingSystem.repository.ShowtimeRepository;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.exception.InvalidOperationException;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final com.dipu.MovieTicketBookingSystem.repository.SeatRepository seatRepository;
    private final com.dipu.MovieTicketBookingSystem.repository.ShowtimeSeatRepository showtimeSeatRepository;

    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        // Calculate end time: movie duration + buffer for cleaning
        LocalDateTime endTime = request.getStartTime()
                .plusMinutes(movie.getDurationMinutes())
                .plusMinutes(AppConstants.CLEANING_BUFFER_MINUTES);

        // Check for overlapping showtimes on the same screen
        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                screen.getId(), request.getStartTime(), endTime);

        if (!overlappingShowtimes.isEmpty()) {
            throw new InvalidOperationException("Screen is already booked for this time period");
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .screen(screen)
                .startTime(request.getStartTime())
                .endTime(endTime)
                .price(request.getPrice())
                .build();

        Showtime savedShowtime = showtimeRepository.save(showtime);

        // Auto-generate ShowtimeSeat entries for every physical seat in this screen
        List<com.dipu.MovieTicketBookingSystem.model.entity.Seat> seats = seatRepository.findByScreenId(screen.getId());
        List<com.dipu.MovieTicketBookingSystem.model.entity.ShowtimeSeat> showtimeSeats = seats.stream()
                .map(seat -> com.dipu.MovieTicketBookingSystem.model.entity.ShowtimeSeat.builder()
                        .showtime(savedShowtime)
                        .seat(seat)
                        .status(com.dipu.MovieTicketBookingSystem.model.enums.SeatStatus.AVAILABLE)
                        .build())
                .collect(Collectors.toList());
        showtimeSeatRepository.saveAll(showtimeSeats);

        return mapToResponse(savedShowtime);
    }

    public List<ShowtimeResponse> getShowtimesByMovie(UUID movieId) {
        return showtimeRepository.findByMovieIdAndStartTimeAfterAndIsActiveTrueOrderByStartTimeAsc(movieId, LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ShowtimeResponse> getShowtimesByTheater(UUID theaterId) {
        return showtimeRepository.findFutureShowtimesByTheater(theaterId, LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ShowtimeResponse updateShowtime(UUID id, ShowtimeRequest request) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));

        // Calculate end time
        LocalDateTime endTime = request.getStartTime()
                .plusMinutes(movie.getDurationMinutes())
                .plusMinutes(AppConstants.CLEANING_BUFFER_MINUTES);

        // Check for overlapping showtimes on the same screen (excluding this showtime itself)
        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                screen.getId(), request.getStartTime(), endTime);
        
        boolean hasOverlap = overlappingShowtimes.stream().anyMatch(s -> !s.getId().equals(showtime.getId()));
        if (hasOverlap) {
            throw new InvalidOperationException("Screen is already booked for this time period");
        }

        showtime.setMovie(movie);
        showtime.setScreen(screen);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setPrice(request.getPrice());

        Showtime updatedShowtime = showtimeRepository.save(showtime);
        return mapToResponse(updatedShowtime);
    }

    public void deleteShowtime(UUID id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        
        List<com.dipu.MovieTicketBookingSystem.model.entity.ShowtimeSeat> seats = showtimeSeatRepository.findByShowtimeId(id);
        boolean hasBookings = seats.stream().anyMatch(s -> s.getStatus() == com.dipu.MovieTicketBookingSystem.model.enums.SeatStatus.BOOKED);
        
        if (hasBookings) {
            throw new InvalidOperationException("Cannot delete showtime because it has active bookings.");
        }
        
        showtime.setActive(false);
        showtimeRepository.save(showtime);
    }

    private ShowtimeResponse mapToResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovie().getId())
                .movieTitle(showtime.getMovie().getTitle())
                .screenId(showtime.getScreen().getId())
                .screenName(showtime.getScreen().getName())
                .theaterId(showtime.getScreen().getTheater().getId())
                .theaterName(showtime.getScreen().getTheater().getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .price(showtime.getPrice())
                .build();
    }
}
