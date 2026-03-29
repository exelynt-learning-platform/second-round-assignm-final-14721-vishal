package com.example.e_commerce_system.service;

import com.example.e_commerce_system.dto.AuthRequest;
import com.example.e_commerce_system.dto.AuthResponse;
import com.example.e_commerce_system.dto.RegisterRequest;
import com.example.e_commerce_system.entity.Role;
import com.example.e_commerce_system.entity.User;
import com.example.e_commerce_system.exception.ResourceNotFoundException;
import com.example.e_commerce_system.repository.UserRepository;
import com.example.e_commerce_system.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

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
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // 🔥 This is the most important part
        if (request.getRole() != null && "ADMIN".equalsIgnoreCase(request.getRole().trim())) {
            user.setRole(Role.ADMIN);
            System.out.println("Admin role assigned to user: " + request.getEmail());
        } else {
            user.setRole(Role.USER);
            System.out.println("User role assigned to user: " + request.getEmail());
        }

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(token, savedUser.getEmail(), savedUser.getRole().name());
    }
    

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}