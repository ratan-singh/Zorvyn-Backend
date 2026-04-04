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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // ─── Helper builders ────────────────────────────────────────────

    private User buildActiveAdmin() {
        return User.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .name("Admin User")
                .email("admin@zorvyn.com")
                .passwordHash("$2a$12$hashedPasswordValue")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();
    }

    private RegisterRequest buildRegisterRequest(String name, String email, String password, Role role) {
        return RegisterRequest.builder()
                .name(name)
                .email(email)
                .password(password)
                .role(role)
                .build();
    }

    private LoginRequest buildLoginRequest(String email, String password) {
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    // ─── register() tests ───────────────────────────────────────────

    @Test
    @DisplayName("register: successful registration returns AuthResponse with JWT token")
    void register_Success_ReturnsAuthResponseWithToken() {
        RegisterRequest request = buildRegisterRequest(
                "Admin User", "admin@zorvyn.com", "Admin@1234", Role.ADMIN);
        User savedUser = buildActiveAdmin();

        when(userRepository.existsByEmail("admin@zorvyn.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin@1234")).thenReturn("$2a$12$hashedPasswordValue");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("mock.jwt.token");
        when(jwtService.getTokenExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("admin@zorvyn.com");
        assertThat(response.getName()).isEqualTo("Admin User");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(savedUser.getId());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: duplicate email throws DuplicateResourceException and never saves")
    void register_DuplicateEmail_ThrowsDuplicateResourceException() {
        RegisterRequest request = buildRegisterRequest(
                "Duplicate User", "existing@zorvyn.com", "Pass@1234", Role.VIEWER);

        when(userRepository.existsByEmail("existing@zorvyn.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing@zorvyn.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: null role defaults to VIEWER")
    void register_DefaultRoleIsViewer_WhenRoleNotProvided() {
        RegisterRequest request = buildRegisterRequest(
                "Viewer User", "viewer@zorvyn.com", "Pass@1234", null);
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("Viewer User")
                .email("viewer@zorvyn.com")
                .passwordHash("$2a$12$encoded")
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.existsByEmail("viewer@zorvyn.com")).thenReturn(false);
        when(passwordEncoder.encode("Pass@1234")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("viewer.jwt.token");
        when(jwtService.getTokenExpirationMs()).thenReturn(86400000L);

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getRole()).isEqualTo(Role.VIEWER);
    }

    // ─── login() tests ──────────────────────────────────────────────

    @Test
    @DisplayName("login: successful authentication returns AuthResponse with valid token")
    void login_Success_ReturnsAuthResponseWithValidToken() {
        LoginRequest request = buildLoginRequest("admin@zorvyn.com", "Admin@1234");
        User existingUser = buildActiveAdmin();

        when(userRepository.findByEmail("admin@zorvyn.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Admin@1234", "$2a$12$hashedPasswordValue")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("valid.jwt.token");
        when(jwtService.getTokenExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("valid.jwt.token");
        assertThat(response.getName()).isEqualTo("Admin User");
        assertThat(response.getEmail()).isEqualTo("admin@zorvyn.com");
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("login: non-existent email throws ResourceNotFoundException without checking password")
    void login_UserNotFound_ThrowsResourceNotFoundException() {
        LoginRequest request = buildLoginRequest("ghost@zorvyn.com", "any");

        when(userRepository.findByEmail("ghost@zorvyn.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ghost@zorvyn.com");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("login: wrong password throws UnauthorizedOperationException without generating token")
    void login_WrongPassword_ThrowsUnauthorizedOperationException() {
        LoginRequest request = buildLoginRequest("admin@zorvyn.com", "WrongPassword");
        User existingUser = buildActiveAdmin();

        when(userRepository.findByEmail("admin@zorvyn.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPassword", "$2a$12$hashedPasswordValue")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login: inactive user throws UnauthorizedOperationException with deactivation message")
    void login_InactiveUser_ThrowsUnauthorizedOperationException() {
        LoginRequest request = buildLoginRequest("inactive@zorvyn.com", "Pass@1234");
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .name("Inactive User")
                .email("inactive@zorvyn.com")
                .passwordHash("$2a$12$hashedValue")
                .role(Role.ANALYST)
                .status(UserStatus.INACTIVE)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        when(userRepository.findByEmail("inactive@zorvyn.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("Pass@1234", "$2a$12$hashedValue")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("deactivated");

        verify(jwtService, never()).generateToken(any());
    }
}
