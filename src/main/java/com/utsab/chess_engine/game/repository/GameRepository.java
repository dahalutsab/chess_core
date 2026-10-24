package com.utsab.chess_engine.game.repository;

import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    Page<Game> findByWhitePlayerIdOrBlackPlayerIdOrderByStartTimeDesc(
            Long whitePlayerId, Long blackPlayerId, Pageable pageable);

    // Statistics over all of a player's games, not just one page of history

    @Query("""
            select count(g) from Game g
            where (g.whitePlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.WHITE_WIN)
               or (g.blackPlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.BLACK_WIN)
            """)
    long countWins(@Param("playerId") Long playerId);

    @Query("""
            select count(g) from Game g
            where (g.whitePlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.BLACK_WIN)
               or (g.blackPlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.WHITE_WIN)
            """)
    long countLosses(@Param("playerId") Long playerId);

    @Query("""
            select count(g) from Game g
            where (g.whitePlayer.id = :playerId or g.blackPlayer.id = :playerId) and g.result in :results
            """)
    long countWithResults(@Param("playerId") Long playerId, @Param("results") Collection<GameResult> results);

    /** Whether the player has a game in progress that was started after the given time */
    @Query("""
            select count(g) > 0 from Game g
            where (g.whitePlayer.id = :playerId or g.blackPlayer.id = :playerId)
              and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.IN_PROGRESS
              and g.startTime > :since
            """)
    boolean hasGameInProgressSince(@Param("playerId") Long playerId, @Param("since") LocalDateTime since);
}
