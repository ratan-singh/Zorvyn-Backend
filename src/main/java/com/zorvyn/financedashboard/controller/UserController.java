package com.zorvyn.financedashboard.controller;

import com.zorvyn.financedashboard.dto.request.UpdateUserRoleRequest;
import com.zorvyn.financedashboard.dto.response.ApiResponse;
import com.zorvyn.financedashboard.dto.response.UserResponse;
import com.zorvyn.financedashboard.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin-only user management — list, view, update roles, activate/deactivate")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "List all users",
            description = "Returns all registered users. ADMIN only."
    )
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get user by ID",
            description = "Returns a single user's details. ADMIN only."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update a user's role",
            description = "Changes a user's role. Admins cannot change their own role. ADMIN only."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cannot change own role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        UserResponse updatedUser = userService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updatedUser));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Deactivate a user account",
            description = "Sets user status to DEACTIVATED. Prevents login. Admins cannot deactivate themselves. ADMIN only."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User deactivated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Cannot deactivate self or already deactivated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable UUID id) {
        UserResponse deactivatedUser = userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", deactivatedUser));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Activate a user account",
            description = "Sets user status to ACTIVE. Re-enables login. ADMIN only."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User activated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User is already active"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable UUID id) {
        UserResponse activatedUser = userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated successfully", activatedUser));
    }
}
