package com.utsab.chess_engine.social.repository;

import com.utsab.chess_engine.social.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    List<Challenge> findByOpponentIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Long opponentId, Challenge.Status status, LocalDateTime after);

    List<Challenge> findByChallengerIdAndCreatedAtAfterOrderByCreatedAtDesc(Long challengerId, LocalDateTime after);

    List<Challenge> findByChallengerIdAndOpponentIdAndStatus(Long challengerId, Long opponentId, Challenge.Status status);
}
