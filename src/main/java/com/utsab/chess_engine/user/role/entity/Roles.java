package com.utsab.chess_engine.user.role.entity;

import com.utsab.chess_engine.common.Auditable;
import jakarta.persistence.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Builder
@Table(name="roles")
public class Roles extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique=true, nullable=false)
    private String name;

    private String description;
}
