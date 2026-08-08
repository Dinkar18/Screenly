package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.AuthRequest;
import com.dipu.MovieTicketBookingSystem.dto.AuthResponse;
import com.dipu.MovieTicketBookingSystem.dto.RegisterRequest;
import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.entity.VerificationToken;
import com.dipu.MovieTicketBookingSystem.model.enums.Role;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import com.dipu.MovieTicketBookingSystem.repository.VerificationTokenRepository;
import com.dipu.MovieTicketBookingSystem.security.CustomUserDetails;
import com.dipu.MovieTicketBookingSystem.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private VerificationTokenRepository verificationTokenRepository;
    
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.CUSTOMER)
                .isVerified(false)
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        String result = authService.register(request);

        assertNotNull(result);
        assertTrue(result.contains("registered successfully"));
        verify(userRepository, times(1)).save(any(User.class));
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendEmail(eq("newuser@example.com"), anyString(), anyString());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        testUser.setVerified(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertEquals("Email already registered", exception.getMessage());
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_EmailExistsButNotVerified_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Updated User");
        request.setEmail("test@example.com");
        request.setPassword("newpassword");

        // isVerified is false by default
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        String result = authService.register(request);

        assertNotNull(result);
        assertTrue(result.contains("registered successfully"));
        verify(userRepository, times(1)).save(testUser);
        verify(verificationTokenRepository, times(1)).deleteByUser(testUser);
        verify(verificationTokenRepository, times(1)).save(any(VerificationToken.class));
        verify(emailService, times(1)).sendEmail(eq("test@example.com"), anyString(), anyString());
    }

    @Test
    void verifyEmail_Success() {
        VerificationToken token = VerificationToken.builder()
                .token("123456")
                .user(testUser)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        when(verificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        String result = authService.verifyEmail("123456");

        assertTrue(result.contains("Email verified successfully"));
        assertTrue(testUser.isVerified());
        verify(userRepository, times(1)).save(testUser);
        verify(verificationTokenRepository, times(1)).delete(token);
    }

    @Test
    void verifyEmail_TokenExpired_ThrowsException() {
        VerificationToken token = VerificationToken.builder()
                .token("123456")
                .user(testUser)
                .expiryDate(LocalDateTime.now().minusMinutes(10))
                .build();

        when(verificationTokenRepository.findByToken("123456")).thenReturn(Optional.of(token));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.verifyEmail("123456"));
        assertEquals("Verification token has expired", exception.getMessage());
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        testUser.setVerified(true);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_UnverifiedUser_ThrowsException() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // User is not verified by default in setUp()
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("Please verify your email before logging in.", exception.getMessage());
    }
}
