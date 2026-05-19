package com.flowboard.label.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "checklist_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private Long checklistId;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Boolean isCompleted = false;

    private Long assigneeId;
    private LocalDateTime dueDate;
}
