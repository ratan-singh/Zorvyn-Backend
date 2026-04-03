package com.zorvyn.financedashboard.service;

import com.zorvyn.financedashboard.dto.request.UpdateUserRoleRequest;
import com.zorvyn.financedashboard.dto.response.UserResponse;
import com.zorvyn.financedashboard.exception.ResourceNotFoundException;
import com.zorvyn.financedashboard.exception.UnauthorizedOperationException;
import com.zorvyn.financedashboard.model.User;
import com.zorvyn.financedashboard.model.enums.UserStatus;
import com.zorvyn.financedashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = findUserOrThrow(userId);
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserRole(UUID userId, UpdateUserRoleRequest request) {
        User currentUser = getAuthenticatedUser();
        User targetUser = findUserOrThrow(userId);

        // Prevent admin from demoting themselves
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new UnauthorizedOperationException(
                    "You cannot change your own role. Another admin must do this.");
        }

        log.info("Role updated: {} ({}) changed from {} to {} by {}",
                targetUser.getEmail(), targetUser.getId(),
                targetUser.getRole(), request.getRole(),
                currentUser.getEmail());

        targetUser.setRole(request.getRole());
        User savedUser = userRepository.save(targetUser);
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse deactivateUser(UUID userId) {
        User currentUser = getAuthenticatedUser();
        User targetUser = findUserOrThrow(userId);

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new UnauthorizedOperationException(
                    "You cannot deactivate your own account. Another admin must do this.");
        }

        if (targetUser.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedOperationException(
                    "User is already deactivated");
        }

        targetUser.setStatus(UserStatus.INACTIVE);
        User savedUser = userRepository.save(targetUser);

        log.info("User deactivated: {} ({}) by {}",
                targetUser.getEmail(), targetUser.getId(), currentUser.getEmail());

        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse activateUser(UUID userId) {
        User currentUser = getAuthenticatedUser();
        User targetUser = findUserOrThrow(userId);

        if (targetUser.getStatus() == UserStatus.ACTIVE) {
            throw new UnauthorizedOperationException(
                    "User is already active");
        }

        targetUser.setStatus(UserStatus.ACTIVE);
        User savedUser = userRepository.save(targetUser);

        log.info("User activated: {} ({}) by {}",
                targetUser.getEmail(), targetUser.getId(), currentUser.getEmail());

        return mapToResponse(savedUser);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
