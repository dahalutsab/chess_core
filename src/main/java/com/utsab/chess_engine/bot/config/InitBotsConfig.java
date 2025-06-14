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
                new Bots("BlunderBot", "Makes moves that are, well, questionable at best.",
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
                        "Loves L-shaped moves and confusing opponents.",
                        "/bots/images/Knightmare.jpg", "knightmare",
                        4,
                        Arrays.asList(
                                "Did my knight just give you a fright?",
                                "I move in mysterious ways.",
                                "You won't see me coming... or going."
                        ),
                        600
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
                        800
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
                        1000
                ),
                new Bots("Queen Bee", "Commands the board with flair and a touch of sass.",
                        "Regal and sassy, ruling the board with style.",
                        "/bots/images/QueenBee.jpg", "queen-bee",
                        10,
                        Arrays.asList(
                                "Bow before your queen!",
                                "I make moves and take names.",
                                "You're just a pawn in my game."
                        ),
                        1200
                ),
                new Bots("Kingpin", "The top boss—strategic, calculated, and formidable.",
                        "Mastermind with a penchant for control.",
                        "/bots/images/Kingpin.jpg", "kingpin",
                        12,
                        Arrays.asList(
                                "You're playing checkers; I'm playing chess.",
                                "Every piece moves at my command.",
                                "This board is my empire."
                        ),
                        1400
                )
        );

        for (Bots bot : bots) {
            if (botRepository.findBySlug(bot.getSlug()).isEmpty()) {
                botRepository.save(bot);
            }
        }
    }
}