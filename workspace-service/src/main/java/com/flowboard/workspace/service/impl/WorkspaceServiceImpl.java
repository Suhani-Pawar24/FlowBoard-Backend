package com.flowboard.workspace.service.impl;

import com.flowboard.workspace.entity.Visibility;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.entity.WorkspaceRole;
import com.flowboard.workspace.entity.SubscriptionTier;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import com.flowboard.workspace.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.flowboard.workspace.client.UserServiceClient;
import com.flowboard.workspace.client.NotificationServiceClient;

@Service
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    private void sendInvitationEmail(Long memberUserId, String workspaceName, Long inviterId) {
        new Thread(() -> {
            try {
                // Fetch invitee details
                Map<String, Object> member = userServiceClient.getUser(memberUserId);
                String email = (String) member.get("email");
                
                // Fetch inviter details
                Map<String, Object> inviter = userServiceClient.getUser(inviterId);
                String inviterName = (String) inviter.get("fullName");

                if (email != null) {
                    Map<String, String> emailRequest = new HashMap<>();
                    emailRequest.put("type", "INVITATION");
                    emailRequest.put("to", email);
                    emailRequest.put("itemName", workspaceName);
                    emailRequest.put("senderName", inviterName != null ? inviterName : "Workspace Admin");

                    notificationServiceClient.sendEmail(emailRequest);
                }
            } catch (Exception e) {
                System.err.println("Failed to send invitation email: " + e.getMessage());
            }
        }).start();
    }

    @Override
    @Transactional
    public Workspace createWorkspace(Workspace workspace) {
        Long currentOwnerId = workspace.getOwnerId();
        
        if (workspaceRepository.existsByNameAndOwnerId(workspace.getName(), currentOwnerId)) {
            throw new RuntimeException("You already have a workspace with this name");
        }
        
        try {
            Workspace savedWorkspace = workspaceRepository.save(workspace);
            addMember(savedWorkspace.getWorkspaceId(), savedWorkspace.getOwnerId(), WorkspaceRole.ADMIN, savedWorkspace.getOwnerId(), "ADMIN");
            return savedWorkspace;
        } catch (Exception e) {
            throw new RuntimeException("Database error: " + e.getMessage());
        }
    }

    @Override
    public Workspace getById(Long id) {
        return workspaceRepository.findByWorkspaceId(id)
                .orElseThrow(() -> new RuntimeException("Workspace not found"));
    }

    @Override
    public List<Workspace> getByOwner(Long ownerId) {
        return workspaceRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Workspace> getByMember(Long userId) {
        return workspaceRepository.findByMemberUserId(userId);
    }

    @Override
    public List<Workspace> getPublicWorkspaces() {
        return workspaceRepository.findByVisibility(Visibility.PUBLIC);
    }

    private void checkPermission(Long workspaceId, Long userId, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) return; // Platform Admin always allowed
        
        Workspace workspace = getById(workspaceId);
        if (workspace.getOwnerId().equals(userId)) return; // Owner always allowed

        // Check if user is a Workspace Admin (not just a member)
        boolean isWorkspaceAdmin = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(m -> m.getRole() == WorkspaceRole.ADMIN)
                .orElse(false);

        if (!isWorkspaceAdmin) {
            throw new RuntimeException("Access Denied: Only workspace owner, workspace admin, or platform admin can perform this action");
        }
    }
    
    private void checkWorkspaceAdmin(Long workspaceId, Long userId) {
        Workspace workspace = getById(workspaceId);
        if (workspace.getOwnerId().equals(userId)) return;

        boolean isWorkspaceAdmin = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(m -> m.getRole() == WorkspaceRole.ADMIN)
                .orElse(false);

        if (!isWorkspaceAdmin) {
            throw new RuntimeException("Access Denied: Only workspace owner or workspace admin can perform this action");
        }
    }

    @Override
    public Workspace updateWorkspace(Long id, Workspace workspace, Long userId, String role) {
        checkPermission(id, userId, role);
        Workspace existing = getById(id);
        existing.setName(workspace.getName());
        existing.setDescription(workspace.getDescription());
        existing.setVisibility(workspace.getVisibility());
        existing.setLogoUrl(workspace.getLogoUrl());
        return workspaceRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long id, Long userId, String role) {
        checkPermission(id, userId, role);
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(id);
        memberRepository.deleteAll(members);
        workspaceRepository.deleteById(id);
    }

    @Override
    public void addMember(Long workspaceId, Long memberUserId, WorkspaceRole memberRole, Long currentUserId, String currentUserRole) {
        Workspace workspace = getById(workspaceId);
        
        // Admin or Workspace Owner/Admin can add members
        boolean isPlatformAdmin = "ADMIN".equals(currentUserRole);
        if (!isPlatformAdmin) {
            checkWorkspaceAdmin(workspaceId, currentUserId);
        }

        // Check tier limits
        long currentMemberCount = memberRepository.findByWorkspaceId(workspaceId).size();
        if (workspace.getTier() == SubscriptionTier.FREE && currentMemberCount >= 5) {
            throw new RuntimeException("Free tier is limited to 5 members. Please upgrade to PRO or ENTERPRISE.");
        } else if (workspace.getTier() == SubscriptionTier.PRO && currentMemberCount >= 50) {
            throw new RuntimeException("Pro tier is limited to 50 members. Please upgrade to ENTERPRISE.");
        }

        if (memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId).isPresent()) {
            return;
        }
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(memberUserId);
        member.setRole(memberRole);
        memberRepository.save(member);
        
        if (!memberUserId.equals(currentUserId)) {
            sendInvitationEmail(memberUserId, workspace.getName(), currentUserId);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long workspaceId, Long memberUserId, Long userId, String role) {
        checkPermission(workspaceId, userId, role);
        Workspace workspace = getById(workspaceId);
        if (workspace.getOwnerId().equals(memberUserId)) {
            throw new RuntimeException("Owner cannot be removed from workspace");
        }
        memberRepository.deleteByWorkspaceIdAndUserId(workspaceId, memberUserId);
    }

    @Override
    public void updateMemberRole(Long workspaceId, Long memberUserId, WorkspaceRole newRole, Long userId, String role) {
        checkPermission(workspaceId, userId, role);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, memberUserId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        member.setRole(newRole);
        memberRepository.save(member);
    }

    @Override
    public List<WorkspaceMember> getMembers(Long workspaceId) {
        return memberRepository.findByWorkspaceId(workspaceId);
    }

    @Override
    public Map<String, Object> getWorkspaceAnalytics(Long workspaceId, Long userId, String role) {
        checkPermission(workspaceId, userId, role);
        Workspace workspace = getById(workspaceId);
        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("workspaceId", workspaceId);
        analytics.put("workspaceName", workspace.getName());
        analytics.put("ownerId", workspace.getOwnerId());
        analytics.put("totalMembers", members.size());
        return analytics;
    }

    @Override
    public List<Workspace> getAllWorkspaces() {
        return workspaceRepository.findAll();
    }

    @Override
    @Transactional
    public void updateWorkspaceTier(Long workspaceId, String tier) {
        Workspace workspace = getById(workspaceId);
        try {
            workspace.setTier(SubscriptionTier.valueOf(tier));
            workspaceRepository.save(workspace);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid subscription tier");
        }
    }
}
