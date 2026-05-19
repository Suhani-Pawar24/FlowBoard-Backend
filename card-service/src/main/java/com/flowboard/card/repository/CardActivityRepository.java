package com.flowboard.card.repository;

import com.flowboard.card.entity.CardActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardActivityRepository extends JpaRepository<CardActivity, Long> {
    List<CardActivity> findByCardIdOrderByTimestampDesc(Long cardId);
}
