package com.utsab.chess_engine.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameMessageRepository extends JpaRepository<GameMessage, Long> {
    List<GameMessage> findTop200ByGameIdOrderByCreatedAtAsc(UUID gameId);
}
