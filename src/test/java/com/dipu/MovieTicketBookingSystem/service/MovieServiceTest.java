package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.MovieRequest;
import com.dipu.MovieTicketBookingSystem.dto.MovieResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Movie;
import com.dipu.MovieTicketBookingSystem.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie testMovie;
    private UUID movieId;

    @BeforeEach
    void setUp() {
        movieId = UUID.randomUUID();
        testMovie = Movie.builder()
                .id(movieId)
                .title("Inception")
                .description("A mind-bending thriller")
                .genre("Sci-Fi")
                .durationMinutes(148)
                .language("English")
                .releaseDate(LocalDate.of(2010, 7, 16))
                .build();
    }

    @Test
    void createMovie_Success() {
        MovieRequest request = new MovieRequest();
        request.setTitle("Inception");
        request.setDescription("A mind-bending thriller");
        request.setGenre("Sci-Fi");
        request.setDurationMinutes(148);
        request.setLanguage("English");
        request.setReleaseDate(LocalDate.of(2010, 7, 16));

        when(movieRepository.save(any(Movie.class))).thenReturn(testMovie);

        MovieResponse response = movieService.createMovie(request);

        assertNotNull(response);
        assertEquals(movieId, response.getId());
        assertEquals("Inception", response.getTitle());
        verify(movieRepository, times(1)).save(any(Movie.class));
    }

    @Test
    void getMovieById_Success() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));

        MovieResponse response = movieService.getMovieById(movieId);

        assertNotNull(response);
        assertEquals("Inception", response.getTitle());
    }

    @Test
    void getMovieById_NotFound_ThrowsException() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> movieService.getMovieById(movieId));
        assertEquals("Movie not found with id: " + movieId, exception.getMessage());
    }

    @Test
    void getAllMovies_Success() {
        org.springframework.data.domain.Page<Movie> page = new org.springframework.data.domain.PageImpl<>(List.of(testMovie));
        when(movieRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        com.dipu.MovieTicketBookingSystem.dto.PageResponse<MovieResponse> responses = movieService.getAllMovies(org.springframework.data.domain.PageRequest.of(0, 10));

        assertNotNull(responses);
        assertEquals(1, responses.getContent().size());
        assertEquals("Inception", responses.getContent().get(0).getTitle());
    }

    @Test
    void updateMovie_Success() {
        MovieRequest request = new MovieRequest();
        request.setTitle("Inception Updated");
        request.setDescription("Updated description");
        request.setGenre("Action");

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        
        Movie updatedMovie = Movie.builder()
                .id(movieId)
                .title("Inception Updated")
                .description("Updated description")
                .genre("Action")
                .build();
                
        when(movieRepository.save(any(Movie.class))).thenReturn(updatedMovie);

        MovieResponse response = movieService.updateMovie(movieId, request);

        assertNotNull(response);
        assertEquals("Inception Updated", response.getTitle());
        assertEquals("Action", response.getGenre());
    }

    @Test
    void deleteMovie_Success() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));

        movieService.deleteMovie(movieId);

        verify(movieRepository, times(1)).delete(testMovie);
    }
}
