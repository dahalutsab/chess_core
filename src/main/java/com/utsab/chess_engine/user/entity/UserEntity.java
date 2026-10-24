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
import java.time.LocalDateTime;
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

    /**
     * Elo rating. The rating columns are nullable so that accounts created before ratings existed need no migration;
     * use {@link #currentRating()} and {@link #ratedGameCount()} to read them.
     */
    @Column(name = "rating")
    private Integer rating;

    @Column(name = "rated_games")
    private Integer ratedGames;

    /** Guest accounts are created on demand for people who play without signing up */
    @Column(name = "is_guest")
    private Boolean guest;

    /** When the user last had the app open, for showing friends as online */
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public static final int DEFAULT_RATING = 1200;

    public int currentRating() {
        return rating != null ? rating : DEFAULT_RATING;
    }

    public int ratedGameCount() {
        return ratedGames != null ? ratedGames : 0;
    }

    public boolean isGuestAccount() {
        return Boolean.TRUE.equals(guest);
    }
}