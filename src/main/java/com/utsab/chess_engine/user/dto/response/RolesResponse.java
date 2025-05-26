package com.utsab.chess_engine.user.dto.response;

import com.utsab.chess_engine.user.role.entity.Roles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class RolesResponse {
    private String role;
    private String description;

    public RolesResponse(Roles role) {
        this.role = role.getName();
        this.description = role.getDescription();
    }
}
