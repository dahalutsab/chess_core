package com.utsab.chess_engine.negamax.service;

import com.utsab.chess_engine.game.dto.GameDto;
import com.utsab.chess_engine.game.dto.history.HistoryResponse;
import com.utsab.chess_engine.negamax.dto.NewGameRequest;
import com.utsab.chess_engine.negamax.dto.UChessRequest;
import com.utsab.chess_engine.negamax.dto.UChessResponse;
import com.utsab.chess_engine.negamax.dto.PreferredColor;
import com.utsab.chess_engine.user.entity.UserEntity;
import org.springframework.data.domain.Pageable;

import java.util.List;

import java.util.UUID;

public interface UChessService {
    GameDto startNewGame(NewGameRequest request);

    UChessResponse getBestMove(UChessRequest uChessRequest);

    GameDto getGameInfo(UUID gameId);

    HistoryResponse getGameHistory(Pageable pageable);

    /** Resign the logged-in user's game */
    GameDto resign(UUID gameId);

    /** Start a game between two users, e.g. when a challenge is accepted. Returns the new game's id. */
    UUID startPvpGame(UserEntity challenger, UserEntity opponent, PreferredColor challengerColor, String timeControl);

    /** Offer a draw in the logged-in user's game, or accept the opponent's offer */
    GameDto offerDraw(UUID gameId);

    /** Decline the opponent's draw offer */
    GameDto declineDraw(UUID gameId);

    /** End games that were left unfinished. Returns how many were ended. */
    int sweepAbandonedGames();

    /** The logged-in user's rating after each of their rated games, oldest first */
    List<RatingPoint> getRatingHistory();

    record RatingPoint(String date, int rating) {}
}
