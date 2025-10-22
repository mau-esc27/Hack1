package org.ide.hack1.controller;

import jakarta.validation.Valid;
import org.ide.hack1.dto.auth.LoginRequest;
import org.ide.hack1.dto.auth.LoginResponse;
import org.ide.hack1.dto.auth.RegisterRequest;
import org.ide.hack1.dto.auth.UserResponse;
import org.ide.hack1.entity.User;
import org.ide.hack1.service.auth.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        User created = authService.register(req);
        UserResponse resp = authService.toUserResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse resp = authService.login(req);
        return ResponseEntity.ok(resp);
    }
}
