package com.utsab.chess_engine.bot.repository;

import com.utsab.chess_engine.bot.entity.Bots;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BotRepository extends JpaRepository<Bots, Long> {
    Optional<Bots> findBySlug(String slug);
}
