package com.zorvyn.financedashboard.dto.request;

import com.zorvyn.financedashboard.model.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required (VIEWER, ANALYST, or ADMIN)")
    private Role role;
}
