package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.MovieRequest;
import com.dipu.MovieTicketBookingSystem.dto.MovieResponse;
import com.dipu.MovieTicketBookingSystem.model.entity.Movie;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.dipu.MovieTicketBookingSystem.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @CacheEvict(value = "movies", allEntries = true)
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .genre(request.getGenre())
                .durationMinutes(request.getDurationMinutes())
                .language(request.getLanguage())
                .releaseDate(request.getReleaseDate())
                .posterUrl(request.getPosterUrl())
                .build();
        
        Movie savedMovie = movieRepository.save(movie);
        return mapToResponse(savedMovie);
    }

    @Cacheable(value = "movies", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<MovieResponse> getAllMovies(Pageable pageable) {
        Page<Movie> moviePage = movieRepository.findAll(pageable);
        List<MovieResponse> responses = moviePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<MovieResponse>builder()
                .content(responses)
                .pageNumber(moviePage.getNumber())
                .pageSize(moviePage.getSize())
                .totalElements(moviePage.getTotalElements())
                .totalPages(moviePage.getTotalPages())
                .isLast(moviePage.isLast())
                .build();
    }

    public PageResponse<MovieResponse> searchMovies(String title, String genre, String language, Pageable pageable) {
        Specification<Movie> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (title != null && !title.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }
            if (genre != null && !genre.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("genre")), genre.toLowerCase()));
            }
            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("language")), language.toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);
        List<MovieResponse> responses = moviePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<MovieResponse>builder()
                .content(responses)
                .pageNumber(moviePage.getNumber())
                .pageSize(moviePage.getSize())
                .totalElements(moviePage.getTotalElements())
                .totalPages(moviePage.getTotalPages())
                .isLast(moviePage.isLast())
                .build();
    }

    public MovieResponse getMovieById(UUID id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        return mapToResponse(movie);
    }

    @CacheEvict(value = "movies", allEntries = true)
    public MovieResponse updateMovie(UUID id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));

        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setLanguage(request.getLanguage());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setPosterUrl(request.getPosterUrl());

        Movie updatedMovie = movieRepository.save(movie);
        return mapToResponse(updatedMovie);
    }

    @CacheEvict(value = "movies", allEntries = true)
    public void deleteMovie(UUID id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        movieRepository.delete(movie);
    }

    private MovieResponse mapToResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .durationMinutes(movie.getDurationMinutes())
                .language(movie.getLanguage())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .build();
    }
}
