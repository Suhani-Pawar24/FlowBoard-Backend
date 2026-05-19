package com.flowboard.board_service.service.impl;

import com.flowboard.board_service.config.RabbitMQConfig;
import com.flowboard.board_service.dto.NotificationEvent;
import com.flowboard.board_service.entity.*;
import com.flowboard.board_service.repository.BoardMemberRepository;
import com.flowboard.board_service.repository.BoardRepository;
import com.flowboard.board_service.service.BoardService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BoardServiceImpl implements BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardMemberRepository boardMemberRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public Board createBoard(Long workspaceId, Board board, Long creatorId) {
        board.setWorkspaceId(workspaceId);
        board.setCreatedById(creatorId);
        board.setStatus(BoardStatus.OPEN);
        Board saved = boardRepository.save(board);
        // Auto-add creator as ADMIN — internal call, skip permission check
        if (!boardMemberRepository.existsByBoardIdAndUserId(saved.getBoardId(), creatorId)) {
            boardMemberRepository.save(new BoardMember(null, saved.getBoardId(), creatorId, BoardRole.ADMIN, null));
        }
        return saved;
    }

    @Override
    public Board getBoardById(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Board not found"));
    }

    @Override
    public List<Board> getBoardsByWorkspace(Long workspaceId, Long userId, String role) {
        List<Board> allWorkspaceBoards = boardRepository.findByWorkspaceId(workspaceId);
        
        // Admins can see all boards in the workspace
        if ("ADMIN".equalsIgnoreCase(role)) {
            return allWorkspaceBoards;
        }

        // Return PUBLIC boards OR boards where the user is explicit member or creator
        return allWorkspaceBoards.stream()
                .filter(b -> b.getVisibility() == Visibility.PUBLIC ||
                             b.getCreatedById().equals(userId) ||
                             boardMemberRepository.existsByBoardIdAndUserId(b.getBoardId(), userId))
                .toList();
    }

    @Override
    public List<Board> getBoardsByCreator(Long createdById) {
        return boardRepository.findByCreatedById(createdById);
    }

    @Override
    public List<Board> getBoardsByMember(Long userId) {
        List<BoardMember> memberships = boardMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(m -> getBoardById(m.getBoardId()))
                .toList();
    }

    private void checkPermission(Long boardId, Long userId, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) return; // Platform Admin always allowed

        Board board = getBoardById(boardId);
        if (board.getCreatedById().equals(userId)) return; // Creator always allowed

        // Check if user is a Board Admin
        boolean isBoardAdmin = boardMemberRepository.findByBoardIdAndUserId(boardId, userId)
                .map(m -> m.getRole() == BoardRole.ADMIN)
                .orElse(false);

        if (!isBoardAdmin) {
            throw new RuntimeException("Access Denied: Only board creator, board admin, or platform admin can perform this action");
        }
    }

    @Override
    public Board updateBoard(Long boardId, Board board, Long userId, String role) {
        checkPermission(boardId, userId, role);
        Board existing = getBoardById(boardId);
        existing.setName(board.getName());
        existing.setDescription(board.getDescription());
        existing.setVisibility(board.getVisibility());
        existing.setBackground(board.getBackground());
        if (board.getStatus() != null) {
            existing.setStatus(board.getStatus());
        }
        return boardRepository.save(existing);
    }

    @Override
    public Board closeBoard(Long boardId, Long userId, String role) {
        checkPermission(boardId, userId, role);
        Board board = getBoardById(boardId);
        board.setStatus(BoardStatus.CLOSED);
        return boardRepository.save(board);
    }

    @Override
    public Board reopenBoard(Long boardId, Long userId, String role) {
        checkPermission(boardId, userId, role);
        Board board = getBoardById(boardId);
        board.setStatus(BoardStatus.OPEN);
        return boardRepository.save(board);
    }

    @Override
    @Transactional
    public void deleteBoard(Long boardId, Long userId, String role) {
        checkPermission(boardId, userId, role);
        boardMemberRepository.deleteAllByBoardId(boardId);
        boardRepository.deleteById(boardId);
    }

    @Override
    public void addBoardMember(Long boardId, Long memberUserId, BoardRole memberRole, Long userId, String role) {
        // Allow: platform ADMIN, board creator, any existing board member
        boolean isPlatformAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isCreator = getBoardById(boardId).getCreatedById().equals(userId);
        boolean isMember = boardMemberRepository.existsByBoardIdAndUserId(boardId, userId);
        if (!isPlatformAdmin && !isCreator && !isMember) {
            throw new RuntimeException("Access Denied: You must be a member of this board to invite others");
        }
        if (!boardMemberRepository.existsByBoardIdAndUserId(boardId, memberUserId)) {
            boardMemberRepository.save(new BoardMember(null, boardId, memberUserId, memberRole, null));

            // ── Publish async notification event to RabbitMQ ──
            Board board = getBoardById(boardId);
            NotificationEvent event = new NotificationEvent(
                    memberUserId,                                   // recipientId = the invited user
                    userId,                                         // actorId = the inviter
                    "BOARD_INVITE",                                 // type
                    "You've been added to a board",                 // title
                    "You have been added to the board \"" + board.getName() + "\" as a " + memberRole.name() + ".",
                    boardId,                                        // relatedId
                    "BOARD"                                         // relatedType
            );
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_EXCHANGE,
                        RabbitMQConfig.ROUTING_KEY,
                        event
                );
            } catch (Exception e) {
                // Don't fail the member add if RabbitMQ is temporarily unavailable
                System.err.println("[WARN] Could not publish notification event: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void removeBoardMember(Long boardId, Long memberUserId, Long userId, String role) {
        checkPermission(boardId, userId, role);
        Board board = getBoardById(boardId);
        if (board.getCreatedById().equals(memberUserId)) {
            throw new RuntimeException("Board creator cannot be removed");
        }
        boardMemberRepository.deleteByBoardIdAndUserId(boardId, memberUserId);
    }

    @Override
    public void updateBoardMemberRole(Long boardId, Long memberUserId, BoardRole newRole, Long userId, String role) {
        checkPermission(boardId, userId, role);
        BoardMember member = boardMemberRepository.findByBoardIdAndUserId(boardId, memberUserId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.setRole(newRole);
        boardMemberRepository.save(member);
    }

    @Override
    public List<BoardMember> getBoardMembers(Long boardId) {
        return boardMemberRepository.findByBoardId(boardId);
    }

    @Override
    public Map<String, Object> getBoardAnalytics(Long boardId, Long userId, String role) {
        checkPermission(boardId, userId, role);
        Board board = getBoardById(boardId);
        List<BoardMember> members = getBoardMembers(boardId);
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("boardId", boardId);
        analytics.put("name", board.getName());
        analytics.put("memberCount", members.size());
        return analytics;
    }

    @Override
    public List<Board> getAllBoards() {
        return boardRepository.findAll();
    }
}
