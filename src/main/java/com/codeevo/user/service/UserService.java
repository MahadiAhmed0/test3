package com.codeevo.user.service;

import com.codeevo.user.dto.AuthResponse;
import com.codeevo.user.dto.LoginRequest;
import com.codeevo.user.dto.RegisterRequest;
import com.codeevo.user.dto.UserResponse;
import com.codeevo.user.model.User;
import com.codeevo.user.repository.UserRepository;
import com.codeevo.config.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName(), request.getEmail(), encodedPassword);
        user = userRepository.save(user);

        UserResponse userResponse = toUserResponse(user);
        String token = jwtProvider.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, userResponse);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        UserResponse userResponse = toUserResponse(user);
        String token = jwtProvider.generateToken(user.getId(), user.getEmail());

        return new AuthResponse(token, userResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
