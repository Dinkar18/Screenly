package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.ForgotPasswordRequest;
import com.dipu.MovieTicketBookingSystem.dto.ResetPasswordRequest;
import com.dipu.MovieTicketBookingSystem.model.entity.PasswordResetToken;
import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.repository.PasswordResetTokenRepository;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    private String email;

    @BeforeEach
    void setUp() {
        email = "test@example.com";
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("old_password")
                .build();
    }

    @Test
    void processForgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.findByUser(testUser)).thenReturn(Optional.empty());

        String result = passwordResetService.processForgotPassword(request);

        assertNotNull(result);
        assertTrue(result.contains("OTP sent to email"));
        
        verify(passwordResetTokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendEmail(eq(email), anyString(), anyString());
    }

    @Test
    void processForgotPassword_UserNotFound_ThrowsException() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> passwordResetService.processForgotPassword(request));
        assertEquals("User not found with this email", exception.getMessage());
        
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void processResetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("123456");
        request.setNewPassword("new_password");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("123456")
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new_password")).thenReturn("encoded_new_password");

        String result = passwordResetService.processResetPassword(request);

        assertTrue(result.contains("successfully reset"));
        assertEquals("encoded_new_password", testUser.getPassword());
        
        verify(userRepository, times(1)).save(testUser);
        verify(passwordResetTokenRepository, times(1)).delete(token);
    }

    @Test
    void processResetPassword_TokenExpired_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("123456");
        request.setNewPassword("new_password");

        PasswordResetToken token = PasswordResetToken.builder()
                .token("123456")
                .user(testUser)
                .expiryDate(LocalDateTime.now().minusMinutes(10))
                .build();

        when(passwordResetTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> passwordResetService.processResetPassword(request));
        assertEquals("Reset token has expired", exception.getMessage());
        
        verify(passwordResetTokenRepository, times(1)).delete(token);
        verify(userRepository, never()).save(any(User.class));
    }
}
