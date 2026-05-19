package com.flowboard.comment.service.impl;

import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.repository.AttachmentRepository;
import com.flowboard.comment.repository.CommentRepository;
import com.flowboard.comment.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GATEWAY_BASE = "http://localhost:8080";

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
            } catch (Exception e) { /* Non-critical */ }
        }).start();
    }

    @Override
    public Comment addComment(Comment comment) {
        Comment saved = commentRepository.save(comment);

        // Notify parent comment author on reply
        if (comment.getParentCommentId() != null) {
            commentRepository.findById(comment.getParentCommentId()).ifPresent(parent -> {
                if (!parent.getAuthorId().equals(comment.getAuthorId())) {
                    sendNotification(parent.getAuthorId(), "COMMENT",
                        "Someone replied to your comment", null);
                }
            });
        }

        // Notify the card's assignee when a comment is added (if not the commenter)
        new Thread(() -> {
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                // Fetch the card to find assigneeId
                java.util.Map<?, ?> card = restTemplate.getForObject(
                    GATEWAY_BASE + "/cards/" + comment.getCardId(), java.util.Map.class);
                if (card != null) {
                    Object assigneeIdObj = card.get("assigneeId");
                    if (assigneeIdObj != null) {
                        Long assigneeId = ((Number) assigneeIdObj).longValue();
                        // Don't notify the person who wrote the comment
                        if (!assigneeId.equals(comment.getAuthorId())) {
                            sendNotification(assigneeId, "COMMENT",
                                "A new comment was added to your assigned card", null);
                        }
                    }
                }
            } catch (Exception e) {
                // Non-critical — comment is already saved
            }
        }).start();

        messagingTemplate.convertAndSend("/topic/card/" + saved.getCardId(),
            java.util.Map.of("eventType", "COMMENT_ADDED", "entityId", saved.getCommentId()));
        return saved;
    }

    @Override
    public List<Comment> getByCard(Long cardId) {
        return commentRepository.findByCardId(cardId).stream()
                .filter(c -> !c.getIsDeleted())
                .toList();
    }

    @Override
    public Comment getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
    }

    @Override
    public List<Comment> getReplies(Long parentCommentId) {
        return commentRepository.findByParentCommentId(parentCommentId).stream()
                .filter(c -> !c.getIsDeleted())
                .toList();
    }

    @Override
    public Comment updateComment(Long commentId, String content) {
        Comment comment = getCommentById(commentId);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = getCommentById(commentId);
        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this comment");
        }
        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

    @Override
    public Attachment addAttachment(Attachment attachment) {
        return attachmentRepository.save(attachment);
    }

    @Override
    public List<Attachment> getAttachmentsByCard(Long cardId) {
        return attachmentRepository.findByCardId(cardId);
    }

    @Override
    public void deleteAttachment(Long attachmentId) {
        attachmentRepository.deleteById(attachmentId);
    }

    @Override
    public long getCommentCount(Long cardId) {
        return commentRepository.countByCardId(cardId);
    }
}
