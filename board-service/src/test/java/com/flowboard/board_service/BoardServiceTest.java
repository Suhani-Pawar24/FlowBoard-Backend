package com.flowboard.board_service;

import com.flowboard.board_service.entity.*;
import com.flowboard.board_service.repository.BoardMemberRepository;
import com.flowboard.board_service.repository.BoardRepository;
import com.flowboard.board_service.service.impl.BoardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository boardMemberRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BoardServiceImpl boardService;

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board();
        board.setBoardId(1L);
        board.setWorkspaceId(10L);
        board.setName("Development Board");
        board.setStatus(BoardStatus.OPEN);
        board.setVisibility(Visibility.PRIVATE);
        board.setCreatedById(2L);
    }

    @Test
    @DisplayName("BS-01: createBoard saves board and adds creator as admin")
    void createBoard_savesAndAddsMember() {
        when(boardRepository.save(any())).thenReturn(board);
        when(boardMemberRepository.existsByBoardIdAndUserId(any(), any())).thenReturn(false);

        Board result = boardService.createBoard(10L, new Board(), 2L);

        assertEquals(10L, result.getWorkspaceId());
        assertEquals(2L, result.getCreatedById());
        verify(boardRepository).save(any());
        verify(boardMemberRepository).save(any()); // Admin member
    }

    @Test
    @DisplayName("BS-02: closeBoard updates status to CLOSED")
    void closeBoard_updatesStatus() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(boardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Creator (userId=2L) closing the board — permission should pass
        Board result = boardService.closeBoard(1L, 2L, "USER");
        assertEquals(BoardStatus.CLOSED, result.getStatus());
    }

    @Test
    @DisplayName("BS-03: deleteBoard removes members and board")
    void deleteBoard_removesAll() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        // Creator (userId=2L) is allowed to delete
        boardService.deleteBoard(1L, 2L, "USER");
        verify(boardMemberRepository).deleteAllByBoardId(1L);
        verify(boardRepository).deleteById(1L);
    }
}
