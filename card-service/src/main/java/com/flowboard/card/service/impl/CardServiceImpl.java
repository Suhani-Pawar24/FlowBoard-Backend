package com.flowboard.card.service.impl;

import com.flowboard.card.entity.Card;
import com.flowboard.card.entity.CardActivity;
import com.flowboard.card.entity.CardStatus;
import com.flowboard.card.entity.Priority;
import com.flowboard.card.repository.CardActivityRepository;
import com.flowboard.card.repository.CardRepository;
import com.flowboard.card.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CardServiceImpl implements CardService {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardActivityRepository activityRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GATEWAY_BASE = "http://api-gateway:8080";
    private static final String AUTH_SERVICE_URL = "http://user-service:8081/user/";
    private static final String NOTIFICATION_SERVICE_URL = "http://notification-service:8084/api/notifications/email/send";

    private void sendAssignmentEmail(Long assigneeId, String cardTitle, Long assignerId) {
        new Thread(() -> {
            try {
                // Fetch assignee details
                Map<String, Object> assignee = restTemplate.getForObject(AUTH_SERVICE_URL + assigneeId, Map.class);
                String email = (String) assignee.get("email");

                String assignerName = "A team member";
                if (assignerId != null) {
                    try {
                        Map<String, Object> assigner = restTemplate.getForObject(AUTH_SERVICE_URL + assignerId, Map.class);
                        assignerName = (String) assigner.get("fullName");
                    } catch (Exception ignore) {}
                }

                if (email != null) {
                    Map<String, String> emailRequest = java.util.Map.of(
                        "type", "ASSIGNMENT",
                        "to", email,
                        "itemName", cardTitle,
                        "senderName", assignerName
                    );

                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    org.springframework.http.HttpEntity<Map<String, String>> request = new org.springframework.http.HttpEntity<>(emailRequest, headers);

                    restTemplate.postForEntity(NOTIFICATION_SERVICE_URL, request, String.class);
                }
            } catch (Exception e) {
                System.err.println("Failed to send assignment email: " + e.getMessage());
            }
        }).start();
    }

    private void sendNotification(Long userId, String type, String message, String link) {
        new Thread(() -> {
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(
                    Map.of("recipientId", userId, "type", type, "message", message,
                           "relatedType", link != null ? link : "", "isRead", false),
                    headers
                );
                restTemplate.postForEntity(GATEWAY_BASE + "/notifications", request, Void.class);
            } catch (Exception e) { 
                try {
                    java.nio.file.Files.writeString(java.nio.file.Paths.get("c:/Sprint Evaluation/notification_error.log"), "Error: " + e.getMessage());
                } catch(Exception ex) {}
            }
        }).start();
    }

    private void logActivity(Long cardId, Long userId, String action, String details) {
        CardActivity activity = new CardActivity();
        activity.setCardId(cardId);
        activity.setUserId(userId != null ? userId : 0L);
        activity.setAction(action);
        activity.setDetails(details);
        activityRepository.save(activity);
    }

    @Override
    public Card createCard(Card card) {
        List<Card> existing = cardRepository.findByListIdOrderByPositionAsc(card.getListId());
        card.setPosition(existing.size());
        Card saved = cardRepository.save(card);
        logActivity(saved.getCardId(), saved.getCreatedById(), "CARD_CREATED", "Created card: " + saved.getTitle());
        messagingTemplate.convertAndSend("/topic/board/" + saved.getBoardId(), Map.of("eventType", "CARD_CREATED", "entityId", saved.getCardId()));
        return saved;
    }

    @Override
    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));
    }

    @Override
    public List<Card> getCardsByList(Long listId) {
        return cardRepository.findByListIdAndIsArchivedOrderByPositionAsc(listId, false);
    }

    @Override
    public List<Card> getCardsByBoard(Long boardId) {
        return cardRepository.findByBoardIdAndIsArchivedOrderByPositionAsc(boardId, false);
    }

    @Override
    public List<Card> getCardsByAssignee(Long assigneeId) {
        return cardRepository.findByAssigneeId(assigneeId);
    }

    @Override
    public Card updateCard(Long cardId, Card card, Long userId) {
        Card existing = getCardById(cardId);
        Long oldAssignee = existing.getAssigneeId();
        Long newAssignee = card.getAssigneeId();

        // ── Detect & log what specifically changed ──────────────────────────
        List<String> changes = new java.util.ArrayList<>();

        if (card.getTitle() != null && !card.getTitle().equals(existing.getTitle())) {
            changes.add("Renamed card to \"" + card.getTitle() + "\"");
        }
        if (card.getDescription() != null && !card.getDescription().equals(existing.getDescription())) {
            changes.add("Updated description");
        }
        if (card.getPriority() != null && !card.getPriority().equals(existing.getPriority())) {
            changes.add("Changed priority from " + existing.getPriority() + " to " + card.getPriority());
        }
        if (card.getStatus() != null && !card.getStatus().equals(existing.getStatus())) {
            changes.add("Changed status from "
                + existing.getStatus().toString().replace("_", " ")
                + " to " + card.getStatus().toString().replace("_", " "));
        }
        if (!java.util.Objects.equals(card.getDueDate(), existing.getDueDate())) {
            if (card.getDueDate() == null) {
                changes.add("Cleared due date");
            } else {
                changes.add("Set due date to "
                    + card.getDueDate().toLocalDate().toString());
            }
        }
        if (!java.util.Objects.equals(card.getStartDate(), existing.getStartDate())) {
            if (card.getStartDate() == null) {
                changes.add("Cleared start date");
            } else {
                changes.add("Set start date to "
                    + card.getStartDate().toLocalDate().toString());
            }
        }
        if (!java.util.Objects.equals(card.getAssigneeId(), existing.getAssigneeId())) {
            if (card.getAssigneeId() == null) {
                changes.add("Removed assignee");
            } else {
                changes.add("Assigned to user #" + card.getAssigneeId());
            }
        }

        // ── Apply changes ───────────────────────────────────────────────────
        existing.setTitle(card.getTitle());
        existing.setDescription(card.getDescription());
        if (card.getStatus() != null) existing.setStatus(card.getStatus());
        existing.setPriority(card.getPriority());
        existing.setDueDate(card.getDueDate());
        existing.setStartDate(card.getStartDate());
        existing.setCoverColor(card.getCoverColor());
        existing.setAssigneeId(card.getAssigneeId());
        existing.setIsComplete(card.getIsComplete());
        existing.setIsArchived(card.getIsArchived());
        if (card.getPosition() != null) existing.setPosition(card.getPosition());

        Card updated = cardRepository.save(existing);

        // ── Log one activity entry per change ───────────────────────────────
        if (changes.isEmpty()) {
            // No meaningful change detected — skip logging
        } else {
            for (String detail : changes) {
                logActivity(cardId, userId, "CARD_UPDATED", detail);
            }
        }

        // ── Notify new assignee ─────────────────────────────────────────────
        if (newAssignee != null && !newAssignee.equals(oldAssignee)) {
            sendNotification(newAssignee, "ASSIGNMENT",
                "You have been assigned to card: " + updated.getTitle(),
                "/board/" + updated.getBoardId() + "/" + cardId);
            sendAssignmentEmail(newAssignee, updated.getTitle(), userId);
        }

        messagingTemplate.convertAndSend("/topic/board/" + updated.getBoardId(),
            Map.of("eventType", "CARD_UPDATED", "entityId", updated.getCardId()));
        return updated;
    }

    @Override
    @Transactional
    public Card moveCard(Long cardId, Long newListId, Integer newPosition) {
        Card card = getCardById(cardId);
        card.setListId(newListId);
        card.setPosition(newPosition);
        Card saved = cardRepository.save(card);
        logActivity(cardId, null, "CARD_MOVED", "Moved card to list " + newListId);
        messagingTemplate.convertAndSend("/topic/board/" + saved.getBoardId(), Map.of("eventType", "CARD_MOVED", "entityId", cardId));
        return saved;
    }

    @Override
    @Transactional
    public void reorderCards(Long listId, List<Long> cardIds) {
        for (int i = 0; i < cardIds.size(); i++) {
            Card card = cardRepository.findById(cardIds.get(i)).orElse(null);
            if (card != null && card.getListId().equals(listId)) {
                card.setPosition(i);
                cardRepository.save(card);
            }
        }
    }

    @Override
    public void archiveCard(Long cardId) {
        Card card = getCardById(cardId);
        card.setIsArchived(true);
        cardRepository.save(card);
    }

    @Override
    public void unarchiveCard(Long cardId) {
        Card card = getCardById(cardId);
        card.setIsArchived(false);
        cardRepository.save(card);
    }

    @Override
    public void deleteCard(Long cardId) {
        cardRepository.deleteById(cardId);
    }

    @Override
    public Card setAssignee(Long cardId, Long assigneeId) {
        Card card = getCardById(cardId);
        card.setAssigneeId(assigneeId);
        Card saved = cardRepository.save(card);
        if (assigneeId != null) {
            sendNotification(assigneeId, "ASSIGNMENT", "Assigned to card: " + card.getTitle(), null);
            sendAssignmentEmail(assigneeId, card.getTitle(), null);
        }
        return saved;
    }

    @Override
    public Card setPriority(Long cardId, String priority) {
        Card card = getCardById(cardId);
        card.setPriority(Priority.valueOf(priority.toUpperCase()));
        return cardRepository.save(card);
    }

    @Override
    public List<Card> getOverdueCards() {
        return cardRepository.findByDueDateBeforeAndStatusNotAndIsArchived(
            LocalDateTime.now(), CardStatus.DONE, false);
    }

    @Override
    public List<Card> getArchivedCards(Long boardId) {
        return cardRepository.findArchivedCardsByBoardId(boardId);
    }
}
