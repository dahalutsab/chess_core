package com.utsab.chess_engine.common.entity;

import com.utsab.chess_engine.common.Auditable;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Abstract base class for players in a chess game, supporting both human users and bots.
 *
 * @author Utsab Dahal
 */
@Entity
@Table(name = "players")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "player_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class Player  extends Auditable {

    /**
     * Unique identifier for the player.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

}