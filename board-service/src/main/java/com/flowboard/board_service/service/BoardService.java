package com.flowboard.board_service.service;

import com.flowboard.board_service.entity.Board;
import com.flowboard.board_service.entity.BoardMember;
import com.flowboard.board_service.entity.BoardRole;
import java.util.List;
import java.util.Map;

public interface BoardService {
    Board createBoard(Long workspaceId, Board board, Long creatorId);
    Board getBoardById(Long boardId);
    List<Board> getBoardsByWorkspace(Long workspaceId, Long userId, String role);
    List<Board> getBoardsByCreator(Long createdById);
    List<Board> getBoardsByMember(Long userId);
    Board updateBoard(Long boardId, Board board, Long userId, String role);
    Board closeBoard(Long boardId, Long userId, String role);
    Board reopenBoard(Long boardId, Long userId, String role);
    void deleteBoard(Long boardId, Long userId, String role);
    
    void addBoardMember(Long boardId, Long memberUserId, BoardRole memberRole, Long userId, String role);
    void removeBoardMember(Long boardId, Long memberUserId, Long userId, String role);
    void updateBoardMemberRole(Long boardId, Long memberUserId, BoardRole newRole, Long userId, String role);
    List<BoardMember> getBoardMembers(Long boardId);
    Map<String, Object> getBoardAnalytics(Long boardId, Long userId, String role);

    // Admin: get all boards platform-wide
    List<Board> getAllBoards();
}
