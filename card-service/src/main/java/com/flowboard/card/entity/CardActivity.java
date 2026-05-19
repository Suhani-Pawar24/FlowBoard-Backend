package com.flowboard.card.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_activities")
@Data
public class CardActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityId;

    private Long cardId;
    private Long userId;
    private String action; // e.g., CARD_CREATED, ASSIGNEE_CHANGED
    private String details;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
