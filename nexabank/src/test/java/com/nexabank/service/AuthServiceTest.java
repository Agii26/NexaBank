package com.nexabank.service;

import com.nexabank.dto.request.LoginRequest;
import com.nexabank.dto.request.RegisterRequest;
import com.nexabank.dto.response.AuthResponse;
import com.nexabank.exception.DuplicateEmailException;
import com.nexabank.exception.InvalidCredentialsException;
import com.nexabank.model.User;
import com.nexabank.model.enums.Role;
import com.nexabank.repository.UserRepository;
import com.nexabank.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository       userRepository;
    @Mock PasswordEncoder      passwordEncoder;
    @Mock JwtUtil              jwtUtil;
    @Mock UserDetailsService   userDetailsService;
    @Mock AuthenticationManager authenticationManager;
    @Mock RefreshTokenService  refreshTokenService;

    @InjectMocks AuthService authService;

    private User         sampleUser;
    private UserDetails  sampleDetails;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Juan dela Cruz")
                .email("juan@test.com")
                .passwordHash("$2a$hashed")
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        sampleDetails = new org.springframework.security.core.userdetails.User(
                "juan@test.com", "$2a$hashed",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    // ── register ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register → saves user and returns tokens")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Juan dela Cruz");
        req.setEmail("juan@test.com");
        req.setPassword("password123");

        when(userRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userDetailsService.loadUserByUsername("juan@test.com")).thenReturn(sampleDetails);
        when(jwtUtil.generateToken(sampleDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("juan@test.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register → throws DuplicateEmailException when email already exists")
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("juan@test.com");
        req.setPassword("password123");
        req.setFullName("Juan");

        when(userRepository.existsByEmail("juan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("juan@test.com");

        verify(userRepository, never()).save(any());
    }

    // ── login ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login → authenticates and returns tokens")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("juan@test.com");
        req.setPassword("password123");

        when(userRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(sampleUser));
        when(userDetailsService.loadUserByUsername("juan@test.com")).thenReturn(sampleDetails);
        when(jwtUtil.generateToken(sampleDetails)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getEmail()).isEqualTo("juan@test.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login → throws InvalidCredentialsException on bad password")
    void login_badCredentials_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("juan@test.com");
        req.setPassword("wrongpassword");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
