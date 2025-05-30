package com.utsab.chess_engine.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotsRequest {

    @NotBlank (message = "Bot name must not be empty")
    @NotNull (message = "Bot name must not be null")
    private String botName;

    private String botDescription;

    private String personality;

    private MultipartFile botImage;

    private List<String> trashTalks;

    private Integer eloRating;
}
