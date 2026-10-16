package org.example.backend.controller;

import org.example.backend.dto.AuthResponseDto;
import org.example.backend.dto.LoginRequestDto;
import org.example.backend.dto.RegisterRequestDto;
import org.example.backend.dto.UserDto;
import org.example.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto registerRequest) {
        try {
            System.out.println("--> [REGISTER] Intentando registrar a: " + registerRequest.getEmail());

            AuthResponseDto response = userService.register(
                    registerRequest.getUsername(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword()
            );

            System.out.println("--> [REGISTER SUCCESS] Token generado: " +
                    (response != null && response.getToken() != null));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("--> [REGISTER ERROR]: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequest) {
        try {
            AuthResponseDto response = userService.login(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("--> [LOGIN ERROR]: " + e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserInfo(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}