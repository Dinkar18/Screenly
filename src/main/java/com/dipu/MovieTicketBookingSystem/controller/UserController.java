package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.dto.UserResponse;
import com.dipu.MovieTicketBookingSystem.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = UserResponse.builder()
                .id(userDetails.getUser().getId())
                .email(userDetails.getUsername())
                .name(userDetails.getUser().getName())
                .role(userDetails.getUser().getRole().name())
                .build();
        return ResponseEntity.ok(response);
    }
}
