package com.flowboard.label.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_labels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Long labelId;
}
