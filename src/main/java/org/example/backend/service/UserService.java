package org.example.backend.service;

import org.example.backend.config.JwtTokenProvider;
import org.example.backend.dto.AuthResponseDto;
import org.example.backend.dto.UserDto;
import org.example.backend.model.User;
import org.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponseDto register(String username, String email, String rawPassword) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";

        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new RuntimeException("El correo ya está registrado");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));

        User savedUser = userRepository.save(user);

        UserDto userDto = new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());

        // Uso correcto de jwtTokenProvider con ambos parámetros (id, email)
        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getEmail());

        return new AuthResponseDto(token, userDto);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(String email, String rawPassword) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        UserDto userDto = new UserDto(user.getId(), user.getUsername(), user.getEmail());
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        return new AuthResponseDto(token, userDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
}