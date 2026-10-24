package com.utsab.chess_engine.social.repository;

import com.utsab.chess_engine.social.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Friendships and requests involving the user, in either direction */
    @Query("select f from Friendship f where f.requester.id = :userId or f.addressee.id = :userId")
    List<Friendship> findAllInvolving(@Param("userId") Long userId);

    /** The friendship or request between two users, in either direction */
    @Query("""
            select f from Friendship f
            where (f.requester.id = :a and f.addressee.id = :b) or (f.requester.id = :b and f.addressee.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    long countByAddresseeIdAndStatus(Long addresseeId, Friendship.Status status);
}
