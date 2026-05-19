package com.flowboard.card.controller;

import com.flowboard.card.entity.Card;
import com.flowboard.card.entity.CardActivity;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cards")
public class CardResource {

    @Autowired
    private CardService cardService;

    @Autowired
    private CardActivityRepository activityRepository;

    @PostMapping
    public ResponseEntity<Card> createCard(@RequestBody Card card) {
        return ResponseEntity.ok(cardService.createCard(card));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<Card> getCardById(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @GetMapping("/list/{listId}")
    public ResponseEntity<List<Card>> getCardsByList(@PathVariable Long listId) {
        return ResponseEntity.ok(cardService.getCardsByList(listId));
    }

    @GetMapping("/board/{boardId}")
    public ResponseEntity<List<Card>> getCardsByBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(cardService.getCardsByBoard(boardId));
    }

    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<List<Card>> getCardsByAssignee(@PathVariable Long assigneeId) {
        return ResponseEntity.ok(cardService.getCardsByAssignee(assigneeId));
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<Card> updateCard(
            @PathVariable Long cardId,
            @RequestBody Card card,
            @RequestHeader(value = "X-user-id", required = false) Long userId) {
        return ResponseEntity.ok(cardService.updateCard(cardId, card, userId));
    }

    @PutMapping("/{cardId}/move")
    public ResponseEntity<Card> moveCard(@PathVariable Long cardId, @RequestParam Long newListId, @RequestParam Integer position) {
        return ResponseEntity.ok(cardService.moveCard(cardId, newListId, position));
    }

    @PutMapping("/reorder/{listId}")
    public ResponseEntity<Void> reorderCards(@PathVariable Long listId, @RequestBody List<Long> cardIds) {
        cardService.reorderCards(listId, cardIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/archive")
    public ResponseEntity<Void> archiveCard(@PathVariable Long cardId) {
        cardService.archiveCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/unarchive")
    public ResponseEntity<Void> unarchiveCard(@PathVariable Long cardId) {
        cardService.unarchiveCard(cardId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{cardId}/assignee")
    public ResponseEntity<Card> setAssignee(@PathVariable Long cardId, @RequestParam Long userId) {
        return ResponseEntity.ok(cardService.setAssignee(cardId, userId));
    }

    @PutMapping("/{cardId}/priority")
    public ResponseEntity<Card> setPriority(@PathVariable Long cardId, @RequestParam String priority) {
        return ResponseEntity.ok(cardService.setPriority(cardId, priority));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Card>> getOverdueCards() {
        return ResponseEntity.ok(cardService.getOverdueCards());
    }

    @GetMapping("/board/{boardId}/archived")
    public ResponseEntity<List<Card>> getArchivedCards(@PathVariable Long boardId) {
        return ResponseEntity.ok(cardService.getArchivedCards(boardId));
    }

    @GetMapping("/{cardId}/activity")
    public ResponseEntity<List<CardActivity>> getCardActivity(@PathVariable Long cardId) {
        return ResponseEntity.ok(activityRepository.findByCardIdOrderByTimestampDesc(cardId));
    }
}
