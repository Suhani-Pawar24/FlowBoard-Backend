package com.flowboard.workspace.repository;

import com.flowboard.workspace.entity.Visibility;
import com.flowboard.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    List<Workspace> findByOwnerId(Long ownerId);
    Optional<Workspace> findByWorkspaceId(Long workspaceId);
    List<Workspace> findByVisibility(Visibility visibility);
    boolean existsByNameAndOwnerId(String name, Long ownerId);
    long countByOwnerId(Long ownerId);

    @Query("SELECT w FROM Workspace w JOIN WorkspaceMember wm ON w.workspaceId = wm.workspaceId WHERE wm.userId = :userId")
    List<Workspace> findByMemberUserId(@Param("userId") Long userId);
}
