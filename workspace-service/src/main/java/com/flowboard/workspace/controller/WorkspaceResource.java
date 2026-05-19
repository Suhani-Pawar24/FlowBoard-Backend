package com.flowboard.workspace.controller;

import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.entity.WorkspaceRole;
import com.flowboard.workspace.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspaces")
public class WorkspaceResource {

    @Autowired
    private WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<?> createWorkspace(@RequestBody Workspace workspace,
                                           @RequestHeader(value = "X-user-id", required = false) Long userId,
                                           @RequestHeader(value = "X-user-email", required = false) String email) {
        
        System.out.println("Workspace creation attempt. ID: " + userId + " | Email: " + email);
        
        if (userId == null || userId == 0) {
            return ResponseEntity.badRequest().body("User ID is missing. Please log in again.");
        }
        
        workspace.setOwnerId(userId);
        try {
            return ResponseEntity.ok(workspaceService.createWorkspace(workspace));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Creation failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workspace> getWorkspace(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getById(id));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Workspace>> getWorkspacesByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(workspaceService.getByOwner(ownerId));
    }

    @GetMapping("/member/{userId}")
    public ResponseEntity<List<Workspace>> getWorkspacesByMember(@PathVariable Long userId) {
        return ResponseEntity.ok(workspaceService.getByMember(userId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<Workspace>> getPublicWorkspaces() {
        return ResponseEntity.ok(workspaceService.getPublicWorkspaces());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkspace(@PathVariable Long id, 
                                            @RequestBody Workspace workspace,
                                            @RequestHeader("X-user-id") Long userId,
                                            @RequestHeader("X-user-role") String role) {
        try {
            return ResponseEntity.ok(workspaceService.updateWorkspace(id, workspace, userId, role));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkspace(@PathVariable Long id,
                                           @RequestHeader("X-user-id") Long userId,
                                           @RequestHeader("X-user-role") String role) {
        try {
            workspaceService.deleteWorkspace(id, userId, role);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<?> addMember(@PathVariable Long id, 
                                      @RequestParam Long memberUserId, 
                                      @RequestParam WorkspaceRole memberRole,
                                      @RequestHeader("X-user-id") Long userId,
                                      @RequestHeader("X-user-role") String role) {
        try {
            workspaceService.addMember(id, memberUserId, memberRole, userId, role);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(403).body("{\"error\": \"Forbidden\", \"message\": \"" + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("/{id}/members/{memberUserId}")
    public ResponseEntity<?> removeMember(@PathVariable Long id, 
                                         @PathVariable Long memberUserId,
                                         @RequestHeader("X-user-id") Long userId,
                                         @RequestHeader("X-user-role") String role) {
        try {
            workspaceService.removeMember(id, memberUserId, userId, role);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/members/{memberUserId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable Long id,
                                               @PathVariable Long memberUserId,
                                               @RequestParam WorkspaceRole newRole,
                                               @RequestHeader("X-user-id") Long userId,
                                               @RequestHeader("X-user-role") String role) {
        try {
            workspaceService.updateMemberRole(id, memberUserId, newRole, userId, role);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMember>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.getMembers(id));
    }

    @GetMapping("/{id}/analytics")
    public ResponseEntity<?> getWorkspaceAnalytics(@PathVariable Long id,
                                                  @RequestHeader("X-user-id") Long userId,
                                                  @RequestHeader("X-user-role") String role) {
        try {
            return ResponseEntity.ok(workspaceService.getWorkspaceAnalytics(id, userId, role));
        } catch (Exception e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    /**
     * GET /workspaces/all — Admin: returns ALL workspaces on the platform.
     * Gated by X-user-role: ADMIN injected by the API Gateway.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllWorkspacesAdmin(
            @RequestHeader(value = "X-user-role", required = false) String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body("Access Denied: Platform Admin role required");
        }
        return ResponseEntity.ok(workspaceService.getAllWorkspaces());
    }

    @PutMapping("/{id}/tier")
    public ResponseEntity<?> updateWorkspaceTier(@PathVariable Long id, @RequestParam String tier) {
        try {
            workspaceService.updateWorkspaceTier(id, tier);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
