package com.dipu.MovieTicketBookingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String title;
    private String description;
    private String genre;
    private Integer durationMinutes;
    private String language;
    private LocalDate releaseDate;
    private String posterUrl;
}
