package com.flowboard.comment.controller;

import com.flowboard.comment.entity.Attachment;
import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
public class CommentResource {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public ResponseEntity<Comment> addComment(@RequestBody Comment comment) {
        return ResponseEntity.ok(commentService.addComment(comment));
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<Comment> getCommentById(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    @GetMapping("/card/{cardId}")
    public ResponseEntity<List<Comment>> getByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getByCard(cardId));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<Comment> updateComment(@PathVariable Long commentId, @RequestBody String content) {
        return ResponseEntity.ok(commentService.updateComment(commentId, content));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/attachments")
    public ResponseEntity<Attachment> addAttachment(@RequestBody Attachment attachment) {
        return ResponseEntity.ok(commentService.addAttachment(attachment));
    }

    @GetMapping("/card/{cardId}/attachments")
    public ResponseEntity<List<Attachment>> getAttachmentsByCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getAttachmentsByCard(cardId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        commentService.deleteAttachment(attachmentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/card/{cardId}/count")
    public ResponseEntity<Long> getCommentCount(@PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.getCommentCount(cardId));
    }
}
