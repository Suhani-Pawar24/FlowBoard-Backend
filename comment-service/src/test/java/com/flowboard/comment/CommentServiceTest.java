package com.flowboard.comment;

import com.flowboard.comment.entity.Comment;
import com.flowboard.comment.repository.CommentRepository;
import com.flowboard.comment.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment comment;

    @BeforeEach
    void setUp() {
        comment = new Comment();
        comment.setCommentId(1L);
        comment.setCardId(10L);
        comment.setAuthorId(2L);
        comment.setContent("Nice work!");
        comment.setIsDeleted(false);
    }

    @Test
    @DisplayName("CM-01: deleteComment performs soft delete")
    void deleteComment_softDelete() {
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        commentService.deleteComment(1L, 2L);
        assertTrue(comment.getIsDeleted());
        verify(commentRepository).save(comment);
    }

    @Test
    @DisplayName("CM-02: getCommentCount returns count from repo")
    void getCommentCount_returnsValue() {
        when(commentRepository.countByCardId(10L)).thenReturn(5L);
        assertEquals(5L, commentService.getCommentCount(10L));
    }
}
