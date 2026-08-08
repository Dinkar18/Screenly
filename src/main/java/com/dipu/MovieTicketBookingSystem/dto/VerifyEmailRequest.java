package com.dipu.MovieTicketBookingSystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "OTP token is required")
    private String token;
}
