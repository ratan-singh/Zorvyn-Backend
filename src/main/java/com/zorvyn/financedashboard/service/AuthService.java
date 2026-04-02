package com.zorvyn.financedashboard.service;

import com.zorvyn.financedashboard.dto.request.LoginRequest;
import com.zorvyn.financedashboard.dto.request.RegisterRequest;
import com.zorvyn.financedashboard.dto.response.AuthResponse;
import com.zorvyn.financedashboard.exception.DuplicateResourceException;
import com.zorvyn.financedashboard.exception.ResourceNotFoundException;
import com.zorvyn.financedashboard.exception.UnauthorizedOperationException;
import com.zorvyn.financedashboard.model.User;
import com.zorvyn.financedashboard.model.enums.Role;
import com.zorvyn.financedashboard.model.enums.UserStatus;
import com.zorvyn.financedashboard.repository.UserRepository;
import com.zorvyn.financedashboard.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with email " + registerRequest.getEmail() + " already exists");
        }

        Role assignedRole = registerRequest.getRole() != null
                ? registerRequest.getRole()
                : Role.VIEWER;

        User newUser = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(assignedRole)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("New user registered: {} with role {}", savedUser.getEmail(), savedUser.getRole());

        String jwtToken = jwtService.generateToken(savedUser);
        return buildAuthResponse(savedUser, jwtToken);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User authenticatedUser = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found with email: " + loginRequest.getEmail()));

        if (!passwordEncoder.matches(loginRequest.getPassword(), authenticatedUser.getPasswordHash())) {
            throw new UnauthorizedOperationException("Invalid email or password");
        }

        if (authenticatedUser.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedOperationException(
                    "Your account has been deactivated. Contact an administrator.");
        }

        log.info("User logged in: {} (role: {})", authenticatedUser.getEmail(), authenticatedUser.getRole());

        String jwtToken = jwtService.generateToken(authenticatedUser);
        return buildAuthResponse(authenticatedUser, jwtToken);
    }

    private AuthResponse buildAuthResponse(User user, String jwtToken) {
        return AuthResponse.builder()
                .token(jwtToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getTokenExpirationMs())
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build();
    }
}
