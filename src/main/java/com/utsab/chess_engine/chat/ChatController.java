package com.utsab.chess_engine.chat;

import com.utsab.chess_engine.common.BaseController;
import com.utsab.chess_engine.events.EventHub;
import com.utsab.chess_engine.exceptions.custom.InvalidRequestException;
import com.utsab.chess_engine.game.entity.Game;
import com.utsab.chess_engine.game.entity.enums.GameType;
import com.utsab.chess_engine.game.repository.GameRepository;
import com.utsab.chess_engine.response.ApiResponse;
import com.utsab.chess_engine.user.entity.UserEntity;
import com.utsab.chess_engine.util.logged_in_user.LoggedInUserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.utsab.chess_engine.common.constants.APIConstants.API_BASE;

/**
 * Chat between the two players of a game against a friend. New messages are pushed on the game's event stream.
 *
 * @author Utsab Dahal
 */
@RestController
@RequestMapping(API_BASE + "/game/{gameId}/chat")
@RequiredArgsConstructor
public class ChatController extends BaseController {

    public record MessageDto(Long id, Long senderId, String senderName, String text, LocalDateTime createdAt) {
        static MessageDto of(GameMessage m) {
            return new MessageDto(m.getId(), m.getSender().getId(), m.getSender().getFullName(), m.getText(), m.getCreatedAt());
        }
    }

    public record NewMessage(String text) {}

    private final GameRepository gameRepository;
    private final GameMessageRepository messageRepository;
    private final LoggedInUserUtil loggedInUserUtil;
    private final EventHub eventHub;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageDto>>> messages(@PathVariable UUID gameId) {
        playersGame(gameId, loggedInUserUtil.getLoggedInUser());
        List<MessageDto> messages = messageRepository.findTop200ByGameIdOrderByCreatedAtAsc(gameId).stream()
                .map(MessageDto::of).toList();
        return successResponse(messages, "Messages fetched successfully");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageDto>> send(@PathVariable UUID gameId, @RequestBody NewMessage body) {
        UserEntity me = loggedInUserUtil.getLoggedInUser();
        Game game = playersGame(gameId, me);
        String text = body == null || body.text() == null ? "" : body.text().strip();
        if (text.isEmpty()) throw new InvalidRequestException("Message is empty");
        if (text.length() > GameMessage.MAX_LENGTH) text = text.substring(0, GameMessage.MAX_LENGTH);

        GameMessage message = messageRepository.save(GameMessage.builder()
                .game(game)
                .sender(me)
                .text(text)
                .createdAt(LocalDateTime.now())
                .build());
        MessageDto dto = MessageDto.of(message);
        eventHub.publish(EventHub.gameTopic(gameId), "chat", dto);
        return successResponse(dto, "Message sent");
    }

    /** A game against a friend that the user plays in; chat isn't offered in bot games */
    private Game playersGame(UUID gameId, UserEntity user) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new InvalidRequestException("Game not found: " + gameId));
        if (game.getGameType() != GameType.PVP) throw new InvalidRequestException("Chat is only available in games against friends");
        boolean player = game.getWhitePlayer().getId().equals(user.getId()) || game.getBlackPlayer().getId().equals(user.getId());
        if (!player) throw new InvalidRequestException("You are not playing in this game");
        return game;
    }
}
