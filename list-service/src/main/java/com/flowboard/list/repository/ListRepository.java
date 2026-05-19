package com.flowboard.list.repository;

import com.flowboard.list.entity.TaskList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListRepository extends JpaRepository<TaskList, Long> {
    List<TaskList> findByBoardId(Long boardId);
    
    @Query("SELECT l FROM TaskList l WHERE l.boardId = :boardId ORDER BY l.position ASC")
    List<TaskList> findByBoardIdOrderByPositionAsc(@Param("boardId") Long boardId);
    
    List<TaskList> findByBoardIdAndIsArchived(Long boardId, Boolean isArchived);
    
    long countByBoardId(Long boardId);
    
    @Query("SELECT MAX(l.position) FROM TaskList l WHERE l.boardId = :boardId")
    Optional<Integer> findMaxPositionByBoardId(@Param("boardId") Long boardId);
    
    void deleteByBoardId(Long boardId);
}
