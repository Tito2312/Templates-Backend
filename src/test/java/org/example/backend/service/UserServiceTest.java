package org.example.backend.service;

import org.example.backend.config.JwtTokenProvider;
import org.example.backend.dto.AuthResponseDto;
import org.example.backend.exception.BadRequestException;
import org.example.backend.exception.ConflictException;
import org.example.backend.model.Role;
import org.example.backend.model.User;
import org.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private UserService userService;

    @Test
    void registerAlwaysCreatesStandardUser() {
        when(passwordEncoder.encode("secret")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(jwtTokenProvider.generateToken(10L, "nuevo@test.com")).thenReturn("token");

        AuthResponseDto response = userService.register(" nuevo ", " Nuevo@Test.com ", "secret");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(Role.ROLE_USER, saved.getValue().getRole());
        assertEquals("nuevo@test.com", saved.getValue().getEmail());
        assertEquals("nuevo", saved.getValue().getUsername());
        assertEquals("ROLE_USER", response.getUser().getRole());
        assertEquals("token", response.getToken());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("dup@test.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.register("u", "dup@test.com", "p"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsernameIgnoreCase("repetido")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.register("repetido", "x@test.com", "p"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsRoleFromDatabase() {
        User admin = User.builder().id(2L).username("admin").email("admin@test.com")
                .password("hash").role(Role.ROLE_ADMIN).build();
        when(userRepository.findByEmailIgnoreCase("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("p", "hash")).thenReturn(true);
        when(jwtTokenProvider.generateToken(2L, "admin@test.com")).thenReturn("token");

        AuthResponseDto response = userService.login("admin@test.com", "p");

        assertEquals("ROLE_ADMIN", response.getUser().getRole());
    }

    @Test
    void loginWithWrongPasswordFails() {
        User user = User.builder().id(1L).email("u@test.com").password("hash").role(Role.ROLE_USER).build();
        when(userRepository.findByEmailIgnoreCase("u@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mala", "hash")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.login("u@test.com", "mala"));
    }
}
