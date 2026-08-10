package com.dipu.MovieTicketBookingSystem.controller;

import com.dipu.MovieTicketBookingSystem.dto.AuthRequest;
import com.dipu.MovieTicketBookingSystem.dto.AuthResponse;
import com.dipu.MovieTicketBookingSystem.dto.RegisterRequest;
import com.dipu.MovieTicketBookingSystem.dto.VerifyEmailRequest;
import com.dipu.MovieTicketBookingSystem.dto.ForgotPasswordRequest;
import com.dipu.MovieTicketBookingSystem.dto.ResetPasswordRequest;
import com.dipu.MovieTicketBookingSystem.service.AuthService;
import com.dipu.MovieTicketBookingSystem.service.PasswordResetService;
import com.dipu.MovieTicketBookingSystem.security.CustomUserDetails;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request.getToken()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.processForgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.processResetPassword(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authReq, HttpServletRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(authReq);
        ResponseCookie springCookie = ResponseCookie.from(AppConstants.COOKIE_NAME_TOKEN, authResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(AppConstants.COOKIE_MAX_AGE_7_DAYS)
                .sameSite("None")
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, springCookie.toString());
        
        return ResponseEntity.ok(authResponse);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie springCookie = ResponseCookie.from(AppConstants.COOKIE_NAME_TOKEN, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
                
        response.addHeader(HttpHeaders.SET_COOKIE, springCookie.toString());
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(AuthResponse.builder()
                .userId(userDetails.getUser().getId())
                .name(userDetails.getUser().getName())
                .email(userDetails.getUser().getEmail())
                .role(userDetails.getUser().getRole().name())
                .token("") // Token is in cookie, not needed in response body anymore
                .build());
    }
}
