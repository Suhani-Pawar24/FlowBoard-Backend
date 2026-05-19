package com.flowboard.card.service;

import com.flowboard.card.entity.Card;
import java.util.List;

public interface CardService {
    Card createCard(Card card);
    Card getCardById(Long cardId);
    List<Card> getCardsByList(Long listId);
    List<Card> getCardsByBoard(Long boardId);
    List<Card> getCardsByAssignee(Long assigneeId);
    Card updateCard(Long cardId, Card card, Long userId);
    Card moveCard(Long cardId, Long newListId, Integer newPosition);
    void reorderCards(Long listId, List<Long> cardIds);
    void archiveCard(Long cardId);
    void unarchiveCard(Long cardId);
    void deleteCard(Long cardId);
    Card setAssignee(Long cardId, Long assigneeId);
    Card setPriority(Long cardId, String priority);
    List<Card> getOverdueCards();
    List<Card> getArchivedCards(Long boardId);
}

