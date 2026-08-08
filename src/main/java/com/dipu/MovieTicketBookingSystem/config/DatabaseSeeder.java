package com.dipu.MovieTicketBookingSystem.config;

import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.enums.Role;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.password:Admin@1234}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@screenly.com").isEmpty()) {
            User admin = User.builder()
                .email("admin@screenly.com")
                .password(passwordEncoder.encode(adminPassword))
                .name("System Admin")
                .role(Role.ADMIN)
                .isVerified(true) // Set to true so you can login immediately
                .build();
            
            userRepository.save(admin);
            System.out.println("\n✅ Default Admin User created successfully!");
            System.out.println("📧 Email: admin@screenly.com");
            System.out.println("🔑 Password: [PROTECTED]\n");
        }
    }
}
