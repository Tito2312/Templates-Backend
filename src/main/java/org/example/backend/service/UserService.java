package org.example.backend.service;

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

    // Inyección por constructor
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDto register(String username, String email, String rawPassword) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";

        if (userRepository.findByEmailIgnoreCase(cleanEmail).isPresent()) {
            throw new RuntimeException("El correo ya se encuentra registrado");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(cleanEmail);
        // Encriptamos la contraseña con BCrypt antes de guardar en DB
        user.setPassword(passwordEncoder.encode(rawPassword));

        User savedUser = userRepository.save(user);
        return new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
    }

    @Transactional(readOnly = true)
    public UserDto login(String email, String rawPassword) {
        String cleanEmail = email != null ? email.trim() : "";

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        // Comparamos la contraseña en texto plano recibida con el hash de la DB
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
}