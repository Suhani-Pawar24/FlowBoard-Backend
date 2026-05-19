package com.flowboard.label.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "labels")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long labelId;
    
    @Column(nullable = false)
    private Long boardId;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String color;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
