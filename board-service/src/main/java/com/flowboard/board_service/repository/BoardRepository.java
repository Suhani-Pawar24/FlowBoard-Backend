package com.flowboard.board_service.repository;

import com.flowboard.board_service.entity.Board;
import com.flowboard.board_service.entity.BoardStatus;
import com.flowboard.board_service.entity.Visibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByWorkspaceId(Long workspaceId);
    List<Board> findByCreatedById(Long createdById);
    List<Board> findByVisibility(Visibility visibility);
    List<Board> findByStatus(BoardStatus status);
    long countByWorkspaceId(Long workspaceId);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Board b JOIN BoardMember bm ON b.boardId = bm.boardId WHERE bm.userId = :userId")
    List<Board> findByMemberUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
