package com.flowboard.label.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checklists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Checklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checklistId;

    @Column(nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private String title;

    private Integer position;

    @OneToMany(mappedBy = "checklistId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecklistItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
