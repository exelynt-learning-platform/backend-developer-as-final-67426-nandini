package com.example.demo.config;

import com.example.demo.entity.Resource;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.ResourceRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedResources();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("user")) {
            User user = User.builder()
                    .username("user")
                    .password(passwordEncoder.encode("User@123"))
                    .role(Role.USER)
                    .build();
            userRepository.save(user);
        }
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            Resource r1 = Resource.builder()
                    .name("Conference Room A")
                    .description("10-person meeting room with projector")
                    .type("ROOM")
                    .pricePerHour(new BigDecimal("500.00"))
                    .active(true)
                    .build();

            Resource r2 = Resource.builder()
                    .name("Conference Room B")
                    .description("20-person meeting room with video conferencing")
                    .type("ROOM")
                    .pricePerHour(new BigDecimal("800.00"))
                    .active(true)
                    .build();

            Resource r3 = Resource.builder()
                    .name("Company Vehicle")
                    .description("Sedan for business trips")
                    .type("VEHICLE")
                    .pricePerHour(new BigDecimal("300.00"))
                    .active(true)
                    .build();

            Resource r4 = Resource.builder()
                    .name("Projector Set")
                    .description("Portable projector with screen")
                    .type("EQUIPMENT")
                    .pricePerHour(new BigDecimal("150.00"))
                    .active(true)
                    .build();

            resourceRepository.saveAll(java.util.List.of(r1, r2, r3, r4));
        }
    }
}