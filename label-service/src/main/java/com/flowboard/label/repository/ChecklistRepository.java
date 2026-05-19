package com.flowboard.label.repository;

import com.flowboard.label.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistRepository extends JpaRepository<Checklist, Long> {
    List<Checklist> findByCardIdOrderByPositionAsc(Long cardId);
}
