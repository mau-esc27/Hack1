package org.ide.hack1.service.auth;

import org.ide.hack1.dto.auth.LoginRequest;
import org.ide.hack1.dto.auth.LoginResponse;
import org.ide.hack1.dto.auth.RegisterRequest;
import org.ide.hack1.dto.auth.UserResponse;
import org.ide.hack1.entity.User;
import org.ide.hack1.exception.ConflictException;
import org.ide.hack1.exception.BadRequestException;
import org.ide.hack1.exception.UnauthorizedException;
import org.ide.hack1.repository.UserRepository;
import org.ide.hack1.security.jwt.JwtProvider;
import org.ide.hack1.security.model.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public User register(RegisterRequest req) {
        // validate uniqueness
        Optional<User> byUsername = userRepository.findByUsername(req.getUsername());
        if (byUsername.isPresent()) {
            throw new ConflictException("username already exists");
        }
        Optional<User> byEmail = userRepository.findByEmail(req.getEmail());
        if (byEmail.isPresent()) {
            throw new ConflictException("email already exists");
        }

        Role role;
        try {
            role = Role.valueOf(req.getRole());
        } catch (Exception ex) {
            throw new BadRequestException("invalid role");
        }

        if (role == Role.BRANCH && (req.getBranch() == null || req.getBranch().isBlank())) {
            throw new BadRequestException("branch is required for BRANCH role");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(role);
        user.setBranch(role == Role.BRANCH ? req.getBranch() : null);

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new UnauthorizedException("invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("invalid credentials");
        }

        String token = jwtProvider.generateToken(user);
        long expiresIn = jwtProvider != null ? Long.parseLong(System.getProperty("jwt.expirationSeconds", "3600")) : 3600L;

        return LoginResponse.builder()
                .token(token)
                .expiresIn(expiresIn)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .branch(user.getBranch())
                .build();
    }

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .branch(user.getBranch())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
