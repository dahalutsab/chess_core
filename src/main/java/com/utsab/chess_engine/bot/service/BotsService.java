package com.utsab.chess_engine.bot.service;

import com.utsab.chess_engine.bot.dto.BotsRequest;
import com.utsab.chess_engine.bot.dto.BotsResponse;

import java.util.List;

public interface BotsService {
    List<BotsResponse> getAllBots();

    BotsResponse getBotBySlug(String botSlug);

    BotsResponse saveBot(BotsRequest botsRequest);

    void deleteBotBySlug(String botSlug);

    BotsResponse updateBot(String botSlug, BotsRequest botsRequest);
}
