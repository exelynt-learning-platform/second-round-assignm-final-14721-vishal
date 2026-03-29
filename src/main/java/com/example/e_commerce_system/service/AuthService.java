package com.example.e_commerce_system.service;

import com.example.e_commerce_system.dto.AuthRequest;
import com.example.e_commerce_system.dto.AuthResponse;
import com.example.e_commerce_system.dto.RegisterRequest;
import com.example.e_commerce_system.entity.Role;
import com.example.e_commerce_system.entity.User;
import com.example.e_commerce_system.exception.EmailAlreadyExistsException;
import com.example.e_commerce_system.exception.ResourceNotFoundException;
import com.example.e_commerce_system.repository.UserRepository;
import com.example.e_commerce_system.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // SECURITY FIX: Prevent privilege escalation
        // Any user registering via public endpoint gets USER role only.
        // Admin accounts should be created manually in database or via a separate secured admin endpoint.
        user.setRole(Role.USER);

        logger.info("New user registered: {} with role: {}", request.getEmail(), Role.USER);

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(token, savedUser.getEmail(), savedUser.getRole().name());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            logger.warn("Failed login attempt for email: {}", request.getEmail());
            throw new RuntimeException("Invalid password"); // Keep simple for login failure (security best practice)
        }

        logger.info("User logged in successfully: {}", request.getEmail());

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}