package com.utsab.chess_engine.move.repository;

import com.utsab.chess_engine.move.entity.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
}
