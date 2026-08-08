package com.dipu.MovieTicketBookingSystem.service;

import com.dipu.MovieTicketBookingSystem.dto.AuthRequest;
import com.dipu.MovieTicketBookingSystem.dto.AuthResponse;
import com.dipu.MovieTicketBookingSystem.dto.RegisterRequest;
import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.enums.Role;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import com.dipu.MovieTicketBookingSystem.security.CustomUserDetails;
import com.dipu.MovieTicketBookingSystem.security.JwtUtil;
import com.dipu.MovieTicketBookingSystem.util.AppConstants;
import com.dipu.MovieTicketBookingSystem.model.entity.VerificationToken;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.dipu.MovieTicketBookingSystem.exception.EmailAlreadyRegisteredException;
import com.dipu.MovieTicketBookingSystem.exception.InvalidTokenException;
import com.dipu.MovieTicketBookingSystem.exception.ResourceNotFoundException;
import com.dipu.MovieTicketBookingSystem.repository.VerificationTokenRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    public String register(RegisterRequest request) {
        var existingUserOpt = userRepository.findByEmail(request.getEmail());

        if (existingUserOpt.isEmpty()) {
            User newUser = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.CUSTOMER)
                    .isVerified(false)
                    .build();
            newUser = userRepository.save(newUser);
            generateAndSendOtp(newUser);
            return "User registered successfully. Please check your email for the OTP.";
        }

        User existingUser = existingUserOpt.get();
        if (existingUser.isVerified()) {
            throw new EmailAlreadyRegisteredException("Email already registered");
        }

        // User exists but not verified, update details and resend OTP
        existingUser.setName(request.getName());
        existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        existingUser = userRepository.save(existingUser);
        
        // Delete old tokens for this user
        verificationTokenRepository.deleteByUser(existingUser);
        generateAndSendOtp(existingUser);

        return "User registered successfully. Please check your email for the OTP.";
    }

    private void generateAndSendOtp(User user) {
        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        
        VerificationToken verificationToken = VerificationToken.builder()
                .token(otp)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES))
                .build();
                
        verificationTokenRepository.save(verificationToken);

        emailService.sendEmail(
                user.getEmail(),
                AppConstants.OTP_EMAIL_SUBJECT,
                "Your OTP for registration is: " + otp + "\nIt will expire in " + AppConstants.OTP_EXPIRY_MINUTES + " minutes."
        );
    }

    public String verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or missing verification token"));

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);
        
        verificationTokenRepository.delete(verificationToken);

        return "Email verified successfully. You can now login.";
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isVerified()) {
            throw new InvalidTokenException("Please verify your email before logging in.");
        }

        var userDetails = new CustomUserDetails(user);
        var jwtToken = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(jwtToken)
                .message("Login successful")
                .build();
    }
}
