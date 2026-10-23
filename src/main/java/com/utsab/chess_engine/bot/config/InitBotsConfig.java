package com.utsab.chess_engine.bot.config;

import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.bot.repository.BotRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class InitBotsConfig {

    private final BotRepository botRepository;

    @PostConstruct
    public void initBots() {
        List<Bots> bots = Arrays.asList(
                new Bots("BlunderBot", "Makes moveEntities that are, well, questionable at best.",
                        "An overconfident beginner who thinks every move is genius.",
                        "/bots/images/BlunderBot.jpg", "blunderbot",
                        2,
                        Arrays.asList(
                                "Oops! Was that my queen?",
                                "I meant to do that... I think.",
                                "Checkmate? Is that a new coffee blend?",
                                "I call that move 'creative chaos'."
                        ),
                        400
                ),
                new Bots("Knightmare", "A play on \"nightmare,\" this bot's tactics can be unexpectedly tricky.",
                        "Loves L-shaped moveEntities and confusing opponents.",
                        "/bots/images/Knightmare.jpg", "knightmare",
                        4,
                        Arrays.asList(
                                "Did my knight just give you a fright?",
                                "I move in mysterious ways.",
                                "You won't see me coming... or going."
                        ),
                        700
                ),
                new Bots("Pawn Star", "Thinks it's the star of the show, but still learning the ropes.",
                        "Aspiring celebrity with a flair for the dramatic.",
                        "/bots/images/PawnStar.jpg", "pawn-star",
                        6,
                        Arrays.asList(
                                "Every pawn has its day!",
                                "I'm ready for my close-up... after this move.",
                                "From humble beginnings to the queen's throne!"
                        ),
                        1100
                ),
                new Bots("Rookie Mistake", "Occasionally makes rookie mistakes, but shows potential.",
                        "Eager learner who sometimes trips over their own pieces.",
                        "/bots/images/RookieMistake.jpg", "rookie-mistake",
                        8,
                        Arrays.asList(
                                "Wait, was that a blunder?",
                                "I swear I had a plan... somewhere.",
                                "Even rookies have their moments!"
                        ),
                        1400
                ),
                new Bots("Queen Bee", "Commands the board with flair and a touch of sass.",
                        "Regal and sassy, ruling the board with style.",
                        "/bots/images/QueenBee.jpg", "queen-bee",
                        10,
                        Arrays.asList(
                                "Bow before your queen!",
                                "I make moveEntities and take names.",
                                "You're just a pawn in my game."
                        ),
                        1600
                ),
                new Bots("Kingpin", "The top boss—strategic, calculated, and formidable.",
                        "Mastermind with a penchant for control.",
                        "/bots/images/Kingpin.jpg", "kingpin",
                        12,
                        Arrays.asList(
                                "You're playing checkers; I'm playing chess.",
                                "Every piece moveEntities at my command.",
                                "This board is my empire."
                        ),
                        1750
                )
        );

        // Ratings were calibrated from a 360-game tournament between the bots (BlunderBot anchored at 400); existing
        // bots are updated so that a database seeded with the old ratings picks up the new values.
        for (Bots bot : bots) {
            botRepository.findBySlug(bot.getSlug()).ifPresent(existing -> {
                if (existing.getEloRating() != bot.getEloRating()) {
                    existing.setEloRating(bot.getEloRating());
                    botRepository.save(existing);
                }
            });
            if (botRepository.findBySlug(bot.getSlug()).isEmpty()) {
                botRepository.save(bot);
            }
        }
    }
}