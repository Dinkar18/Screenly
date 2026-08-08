package com.dipu.MovieTicketBookingSystem.config;

import com.dipu.MovieTicketBookingSystem.model.entity.User;
import com.dipu.MovieTicketBookingSystem.model.enums.Role;
import com.dipu.MovieTicketBookingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@cinereserve.com").isEmpty()) {
            User admin = User.builder()
                .email("admin@cinereserve.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .name("System Admin")
                .role(Role.ADMIN)
                .isVerified(true) // Set to true so you can login immediately
                .build();
            
            userRepository.save(admin);
            System.out.println("\n✅ Default Admin User created successfully!");
            System.out.println("📧 Email: admin@cinereserve.com");
            System.out.println("🔑 Password: Admin@1234\n");
        }
    }
}
