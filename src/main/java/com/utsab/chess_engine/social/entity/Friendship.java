package com.utsab.chess_engine.social.entity;

import com.utsab.chess_engine.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A friend request from one user to another; once accepted, the two are friends.
 *
 * @author Utsab Dahal
 */
@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "addressee_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Friendship {

    public enum Status { PENDING, ACCEPTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private UserEntity requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id")
    private UserEntity addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    /** The other user in this friendship, from the given user's point of view */
    public UserEntity other(Long userId) {
        return requester.getId().equals(userId) ? addressee : requester;
    }
}
