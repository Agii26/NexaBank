package com.nexabank.config;

import com.nexabank.model.User;
import com.nexabank.model.enums.Role;
import com.nexabank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail("admin@nexabank.com").isEmpty()) {
            userRepository.save(User.builder()
                    .fullName("System Admin")
                    .email("admin@nexabank.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.ADMIN)
                    .active(true)
                    .build());
            log.info("✓ Default admin created  →  admin@nexabank.com / Admin@123456");
        }
    }
}
