package com.utsab.chess_engine.rating;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.common.entity.Player;
import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.entity.enums.GameResult;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

/**
 * Updates players' Elo ratings when a game ends.
 * <p>
 * Users are rated; bots keep the fixed rating they were calibrated to, so they act as anchors for the rating pool.
 * Games where either side made no move are not rated, so abandoning a game straight away costs nothing.
 *
 * @author Utsab Dahal
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    /** Rated games after which a player's rating is considered established */
    static final int PROVISIONAL_GAMES = 30;

    private final UserInfoRepository userInfoRepository;

    public static int ratingOf(Player player) {
        Object real = Hibernate.unproxy(player);
        if (real instanceof UserEntity user) return user.currentRating();
        if (real instanceof Bots bot) return bot.getEloRating();
        return UserEntity.DEFAULT_RATING;
    }

    /**
     * Rate a finished game, once: records each side's rating before the game and its change on the game, and updates
     * the users. Does nothing for a game that is still in progress or was rated already.
     */
    public void rate(Game game) {
        if (game.getResult() == null || !game.getResult().isFinished() || game.getWhiteRatingChange() != null) return;

        Object white = Hibernate.unproxy(game.getWhitePlayer());
        Object black = Hibernate.unproxy(game.getBlackPlayer());
        int whiteRating = ratingOf(game.getWhitePlayer());
        int blackRating = ratingOf(game.getBlackPlayer());
        game.setWhiteRatingBefore(whiteRating);
        game.setBlackRatingBefore(blackRating);

        if (game.getMoveEntities().size() < 2) {
            game.setWhiteRatingChange(0);
            game.setBlackRatingChange(0);
            return;
        }

        double whiteScore = game.getResult() == GameResult.WHITE_WIN ? 1 : game.getResult() == GameResult.BLACK_WIN ? 0 : 0.5;
        int whiteChange = white instanceof UserEntity user ? change(user, whiteRating, blackRating, whiteScore) : 0;
        int blackChange = black instanceof UserEntity user ? change(user, blackRating, whiteRating, 1 - whiteScore) : 0;
        game.setWhiteRatingChange(whiteChange);
        game.setBlackRatingChange(blackChange);

        if (white instanceof UserEntity user) apply(user, whiteChange);
        if (black instanceof UserEntity user) apply(user, blackChange);
        log.info("[RatingService] Game {} rated: white {} ({}), black {} ({})",
                game.getId(), whiteRating, whiteChange, blackRating, blackChange);
    }

    /** The Elo change for a player with the given rating scoring {@code score} (1, ½ or 0) against an opponent */
    static int change(int rating, int opponentRating, double score, int kFactor) {
        double expected = 1 / (1 + Math.pow(10, (opponentRating - rating) / 400.0));
        return (int) Math.round(kFactor * (score - expected));
    }

    private static int change(UserEntity user, int rating, int opponentRating, double score) {
        return change(rating, opponentRating, score, kFactor(user));
    }

    /** New players move quickly towards their true strength; established and strong players more slowly */
    static int kFactor(UserEntity user) {
        if (user.ratedGameCount() < PROVISIONAL_GAMES) return 40;
        return user.currentRating() >= 2400 ? 10 : 20;
    }

    private void apply(UserEntity user, int change) {
        user.setRating(Math.max(100, user.currentRating() + change));
        user.setRatedGames(user.ratedGameCount() + 1);
        userInfoRepository.save(user);
    }
}
