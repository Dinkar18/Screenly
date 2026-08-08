package com.dipu.MovieTicketBookingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private java.util.UUID userId;
    private String name;
    private String email;
    private String role;
    private String token;
    private String message;
}
