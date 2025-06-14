package com.utsab.chess_engine.user.entity;

import com.utsab.chess_engine.common.Auditable;
import com.utsab.chess_engine.common.entity.Player;
import com.utsab.chess_engine.user.role.entity.Roles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name="users")
public class UserEntity extends Player{

    @Column(nullable = false, name = "full_name")
    private String fullName;

    @Column(nullable = false, name = "email", unique = true)
    private String emailId;

    @Column(name = "profile")
    private String profileImage;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Roles> roles = new ArrayList<>();

    @Column(name = "password", nullable = false)
    private String password;

    @Column(nullable = false, name = "IS_VERIFIED")
    private boolean isVerified = false;

    @Column(nullable = false, name = "IS_ACTIVE")
    private boolean isActive = true;
}