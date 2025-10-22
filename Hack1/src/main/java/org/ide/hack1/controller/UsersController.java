package org.ide.hack1.controller;

import org.ide.hack1.dto.auth.UserResponse;
import org.ide.hack1.entity.User;
import org.ide.hack1.exception.ForbiddenException;
import org.ide.hack1.exception.NotFoundException;
import org.ide.hack1.repository.UserRepository;
import org.ide.hack1.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UsersController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UsersController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    private String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a.startsWith("ROLE_")) return a.substring(5);
        }
        return null;
    }

    private void ensureCentral() {
        String role = currentRole();
        if (!"CENTRAL".equals(role)) {
            throw new ForbiddenException("only CENTRAL users can perform this action");
        }
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        ensureCentral();
        List<UserResponse> resp = userRepository.findAll().stream()
                .map(authService::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        ensureCentral();
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("user not found"));
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        ensureCentral();
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
