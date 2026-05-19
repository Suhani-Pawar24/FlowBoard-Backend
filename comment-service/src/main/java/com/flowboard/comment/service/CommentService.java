package com.flowboard.comment.service;

import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.entity.Attachment;
import java.util.List;

public interface CommentService {
    Comment addComment(Comment comment);
    List<Comment> getByCard(Long cardId);
    Comment getCommentById(Long commentId);
    List<Comment> getReplies(Long parentCommentId);
    Comment updateComment(Long commentId, String content);
    void deleteComment(Long commentId, Long userId);
    Attachment addAttachment(Attachment attachment);
    List<Attachment> getAttachmentsByCard(Long cardId);
    void deleteAttachment(Long attachmentId);
    long getCommentCount(Long cardId);
}
