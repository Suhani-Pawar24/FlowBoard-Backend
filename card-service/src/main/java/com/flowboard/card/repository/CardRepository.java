package com.flowboard.card.repository;

import com.flowboard.card.entity.Card;
import com.flowboard.card.entity.CardStatus;
import com.flowboard.card.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByListIdOrderByPositionAsc(Long listId);
    List<Card> findByListIdAndIsArchivedOrderByPositionAsc(Long listId, Boolean isArchived);
    List<Card> findByBoardId(Long boardId);
    List<Card> findByBoardIdAndIsArchivedOrderByPositionAsc(Long boardId, boolean isArchived);
    List<Card> findByAssigneeId(Long assigneeId);
    List<Card> findByDueDateBeforeAndStatusNotAndIsArchived(LocalDateTime date, CardStatus status, boolean isArchived);
    List<Card> findByPriority(Priority priority);
    List<Card> findByStatus(CardStatus status);
    long countByListId(Long listId);

    @Query("SELECT c FROM Card c WHERE c.boardId = :boardId AND c.isArchived = true")
    List<Card> findArchivedCardsByBoardId(@Param("boardId") Long boardId);
}
