package com.flowboard.workspace.service;

import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.entity.WorkspaceRole;

import java.util.List;
import java.util.Map;

public interface WorkspaceService {
    Workspace createWorkspace(Workspace workspace);
    Workspace getById(Long id);
    List<Workspace> getByOwner(Long ownerId);
    List<Workspace> getByMember(Long userId);
    List<Workspace> getPublicWorkspaces();
    Workspace updateWorkspace(Long id, Workspace workspace, Long userId, String role);
    void deleteWorkspace(Long id, Long userId, String role);

    // Member management
    void addMember(Long workspaceId, Long memberUserId, WorkspaceRole memberRole, Long userId, String role);
    void removeMember(Long workspaceId, Long memberUserId, Long userId, String role);
    void updateMemberRole(Long workspaceId, Long memberUserId, WorkspaceRole newRole, Long userId, String role);
    List<WorkspaceMember> getMembers(Long workspaceId);

    // Analytics (Case Study Section 4.2)
    Map<String, Object> getWorkspaceAnalytics(Long workspaceId, Long userId, String role);

    // Admin: get all workspaces platform-wide
    List<Workspace> getAllWorkspaces();
    void updateWorkspaceTier(Long workspaceId, String tier);
}
