package com.flowboard.comment.repository;

import com.flowboard.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByCardId(Long cardId);
    List<Comment> findByAuthorId(Long authorId);
    List<Comment> findByParentCommentId(Long parentCommentId);
    long countByCardId(Long cardId);
    void deleteByCommentId(Long commentId);
}
