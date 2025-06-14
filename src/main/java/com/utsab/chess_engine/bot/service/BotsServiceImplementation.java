package com.utsab.chess_engine.bot.service;

import com.utsab.chess_engine.bot.dto.BotsRequest;
import com.utsab.chess_engine.bot.dto.BotsResponse;
import com.utsab.chess_engine.bot.entity.Bots;
import com.utsab.chess_engine.bot.repository.BotRepository;
import com.utsab.chess_engine.util.file.FileHandlerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BotsServiceImplementation implements BotsService {
    private final BotRepository botRepository;
    private final FileHandlerUtil fileHandlerUtil;

    @Override
    public List<BotsResponse> getAllBots() {
        return botRepository.findAll().stream()
                .map(BotsResponse::new)
                .toList();
    }

    @Override
    public BotsResponse getBotBySlug(String botSlug) {
        return botRepository.findBySlug(botSlug)
                .map(BotsResponse::new)
                .orElseThrow(() -> new RuntimeException("Bot not found with slug: " + botSlug));
    }

    @Override
    public BotsResponse saveBot(BotsRequest botsRequest) {
        Bots bot = new Bots();
        bot.setBotName(botsRequest.getBotName());
        bot.setBotDescription(botsRequest.getBotDescription());
        bot.setPersonality(botsRequest.getPersonality());
        bot.setBotImageUrl(fileHandlerUtil.saveFile(botsRequest.getBotImage(), "bots/images/").getFileDownloadUri());
        bot.setSlug(generateUniqueSlug(botsRequest.getBotName()));
        bot.setTrashTalks(botsRequest.getTrashTalks());
        bot.setEloRating(botsRequest.getEloRating());
        Bots savedBot = botRepository.save(bot);
        log.info("Bot saved with ID: {}", savedBot.getId());
        return new BotsResponse(savedBot);
    }

    private String generateUniqueSlug(String botName) {
        String baseSlug = botName.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        String slug = baseSlug;
        int count = 1;

        while (botRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + count++;
        }

        return slug;
    }

    @Override
    public void deleteBotBySlug(String botSlug) {
        Bots bot = botRepository.findBySlug(botSlug)
                .orElseThrow(() -> new RuntimeException("Bot not found with slug: " + botSlug));
        botRepository.delete(bot);
        log.info("Bot deleted with slug: {}", botSlug);
    }

    @Override
    public BotsResponse updateBot(String botSlug, BotsRequest botsRequest) {
        Bots existingBot = botRepository.findBySlug(botSlug)
                .orElseThrow(() -> new RuntimeException("Bot not found with slug: " + botSlug));

        existingBot.setBotName(botsRequest.getBotName());
        existingBot.setBotDescription(botsRequest.getBotDescription());
        existingBot.setPersonality(botsRequest.getPersonality());
        if (botsRequest.getBotImage() != null) {
            existingBot.setBotImageUrl(fileHandlerUtil.saveFile(botsRequest.getBotImage(), "bots/images/").getFileDownloadUri());
        }
        existingBot.setTrashTalks(botsRequest.getTrashTalks());
        existingBot.setEloRating(botsRequest.getEloRating());

        Bots updatedBot = botRepository.save(existingBot);
        log.info("Bot updated with ID: {}", updatedBot.getId());
        return new BotsResponse(updatedBot);
    }

}
