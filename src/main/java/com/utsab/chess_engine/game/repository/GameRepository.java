package com.utsab.chess_engine.game.repository;

import com.utsab.chess_engine.game.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    Page<Game> findByWhitePlayerIdOrBlackPlayerIdOrderByStartTimeDesc(
            Long whitePlayerId, Long blackPlayerId, Pageable pageable);
}
