package org.example.backend.controller;

import org.example.backend.dto.LoginRequestDto;
import org.example.backend.dto.UserDto;
import org.example.backend.model.User;
import org.example.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password) {
        return ResponseEntity.ok(userService.register(username, email, password));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequestDto loginRequest) {
        UserDto user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserInfo(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}