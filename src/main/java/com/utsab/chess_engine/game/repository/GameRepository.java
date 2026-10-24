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
import java.util.List;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
    Page<Game> findByWhitePlayerIdOrBlackPlayerIdOrderByStartTimeDesc(
            Long whitePlayerId, Long blackPlayerId, Pageable pageable);

    // Statistics over all of a player's games, not just one page of history

    @Query("""
            select count(g) from Game g
            where ((g.whitePlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.WHITE_WIN)
               or (g.blackPlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.BLACK_WIN))
              and (g.termination is null or g.termination <> 'ABORTED')
            """)
    long countWins(@Param("playerId") Long playerId);

    @Query("""
            select count(g) from Game g
            where ((g.whitePlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.BLACK_WIN)
               or (g.blackPlayer.id = :playerId and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.WHITE_WIN))
              and (g.termination is null or g.termination <> 'ABORTED')
            """)
    long countLosses(@Param("playerId") Long playerId);

    @Query("""
            select count(g) from Game g
            where (g.whitePlayer.id = :playerId or g.blackPlayer.id = :playerId) and g.result in :results
            """)
    long countWithResults(@Param("playerId") Long playerId, @Param("results") Collection<GameResult> results);

    /** The parts of a finished, rated game needed for a rating chart */
    record RatedGame(LocalDateTime startTime, LocalDateTime endTime, Long whitePlayerId,
                     Integer whiteRatingBefore, Integer whiteRatingChange,
                     Integer blackRatingBefore, Integer blackRatingChange) {}

    /**
     * The player's rated games, newest first. Unrated games (where a side never moved) are left out, as they don't
     * change the rating.
     */
    @Query("""
            select new com.utsab.chess_engine.game.repository.GameRepository$RatedGame(
                g.startTime, g.endTime, g.whitePlayer.id,
                g.whiteRatingBefore, g.whiteRatingChange, g.blackRatingBefore, g.blackRatingChange)
            from Game g
            where (g.whitePlayer.id = :playerId or g.blackPlayer.id = :playerId)
              and g.endTime is not null and g.whiteRatingChange is not null
              and size(g.moveEntities) >= 2
            order by g.startTime desc
            """)
    List<RatedGame> findRatedGames(@Param("playerId") Long playerId, Pageable pageable);

    /** The player's games, leaving out aborted ones */
    @Query("""
            select count(g) from Game g
            where (g.whitePlayer.id = :playerId or g.blackPlayer.id = :playerId)
              and (g.termination is null or g.termination <> 'ABORTED')
            """)
    long countCounted(@Param("playerId") Long playerId);

    /**
     * Games in progress that may have been left unfinished: timed games whose clock is running (the sweep checks
     * whether the player to move has run out of time), and games idle for longer than the abort or abandon limits.
     * Last activity is the newest move, or else when the game last changed (as in the sweep itself).
     */
    @Query("""
            select g.id from Game g
            where g.result = com.utsab.chess_engine.game.entity.enums.GameResult.IN_PROGRESS
              and (
                   (g.baseTimeMs is not null and size(g.moveEntities) >= 2)
                or (size(g.moveEntities) < 2
                    and coalesce((select max(m.createdDate) from MoveEntity m where m.game = g), g.lastModifiedDate, g.startTime) < :abortBefore)
                or (g.baseTimeMs is null and g.gameType = com.utsab.chess_engine.game.entity.enums.GameType.PVP
                    and coalesce((select max(m.createdDate) from MoveEntity m where m.game = g), g.lastModifiedDate, g.startTime) < :pvpBefore)
                or (g.baseTimeMs is null and g.gameType <> com.utsab.chess_engine.game.entity.enums.GameType.PVP
                    and coalesce((select max(m.createdDate) from MoveEntity m where m.game = g), g.lastModifiedDate, g.startTime) < :pvbBefore)
              )
            """)
    List<UUID> findSweepCandidates(@Param("abortBefore") LocalDateTime abortBefore,
                                   @Param("pvpBefore") LocalDateTime pvpBefore,
                                   @Param("pvbBefore") LocalDateTime pvbBefore);

    /** Whether the player has a game in progress that was started after the given time */
    @Query("""
            select count(g) > 0 from Game g
            where (g.whitePlayer.id = :playerId or g.blackPlayer.id = :playerId)
              and g.result = com.utsab.chess_engine.game.entity.enums.GameResult.IN_PROGRESS
              and g.startTime > :since
            """)
    boolean hasGameInProgressSince(@Param("playerId") Long playerId, @Param("since") LocalDateTime since);
}
