package org.example.backend.service;

import org.example.backend.config.JwtTokenProvider;
import org.example.backend.dto.AuthResponseDto;
import org.example.backend.dto.UserDto;
import org.example.backend.exception.BadRequestException;
import org.example.backend.exception.ConflictException;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.model.Role;
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
        String cleanEmail = email.trim().toLowerCase();
        String cleanUsername = username.trim();

        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ConflictException("El correo ya está registrado");
        }
        if (userRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new ConflictException("El nombre de usuario ya está en uso");
        }

        User user = new User();
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        // El registro público siempre crea usuarios estándar
        user.setRole(Role.ROLE_USER);

        User savedUser = userRepository.save(user);

        // Uso correcto de jwtTokenProvider con ambos parámetros (id, email)
        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getEmail());

        return new AuthResponseDto(token, mapToDto(savedUser));
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(String email, String rawPassword) {
        String cleanEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new BadRequestException("Credenciales inválidas"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadRequestException("Credenciales inválidas");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());

        return new AuthResponseDto(token, mapToDto(user));
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }
}
