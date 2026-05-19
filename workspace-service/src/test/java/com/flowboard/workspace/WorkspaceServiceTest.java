package com.flowboard.workspace;

import com.flowboard.workspace.entity.*;
import com.flowboard.workspace.repository.*;
import com.flowboard.workspace.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private com.flowboard.workspace.client.UserServiceClient userServiceClient;
    @Mock private com.flowboard.workspace.client.NotificationServiceClient notificationServiceClient;

    private WorkspaceServiceImpl workspaceService;

    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceServiceImpl();
        injectField(workspaceService, "workspaceRepository", workspaceRepository);
        injectField(workspaceService, "memberRepository", workspaceMemberRepository);
        injectField(workspaceService, "userServiceClient", userServiceClient);
        injectField(workspaceService, "notificationServiceClient", notificationServiceClient);
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Field injection failed: " + fieldName, e);
        }
    }

    @Test
    @DisplayName("WS-01: createWorkspace saves workspace and auto-adds owner as ADMIN")
    void createWorkspace_savesAndAddsOwner() {
        Workspace ws = new Workspace();
        ws.setName("My Workspace");
        ws.setOwnerId(10L);

        when(workspaceRepository.existsByNameAndOwnerId("My Workspace", 10L)).thenReturn(false);
        when(workspaceRepository.save(any())).thenAnswer(inv -> {
            Workspace saved = inv.getArgument(0);
            saved.setWorkspaceId(1L);
            return saved;
        });

        // addMember() calls getById(workspaceId) to check tier limits
        Workspace savedWs = new Workspace();
        savedWs.setWorkspaceId(1L);
        savedWs.setOwnerId(10L);
        savedWs.setTier(com.flowboard.workspace.entity.SubscriptionTier.FREE);
        when(workspaceRepository.findByWorkspaceId(1L)).thenReturn(Optional.of(savedWs));

        // addMember() checks member count and duplicate membership
        when(workspaceMemberRepository.findByWorkspaceId(1L)).thenReturn(Collections.emptyList());
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(1L, 10L)).thenReturn(Optional.empty());

        Workspace result = workspaceService.createWorkspace(ws);

        assertNotNull(result);
        assertEquals("My Workspace", result.getName());
        verify(workspaceMemberRepository).save(any());
    }

    @Test
    @DisplayName("WS-02: updateWorkspace updates fields")
    void updateWorkspace_updatesFields() {
        Workspace existing = new Workspace();
        existing.setWorkspaceId(1L);
        existing.setName("Old");

        Workspace update = new Workspace();
        update.setName("New Name");
        update.setVisibility(Visibility.PUBLIC);

        when(workspaceRepository.findByWorkspaceId(1L)).thenReturn(Optional.of(existing));
        when(workspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Workspace result = workspaceService.updateWorkspace(1L, update, 10L, "ADMIN");
        assertEquals("New Name", result.getName());
        assertEquals(Visibility.PUBLIC, result.getVisibility());
    }

    @Test
    @DisplayName("WS-03: deleteWorkspace removes members and workspace")
    void deleteWorkspace_removesAll() {
        when(workspaceMemberRepository.findByWorkspaceId(1L)).thenReturn(Collections.emptyList());
        workspaceService.deleteWorkspace(1L, 10L, "ADMIN");
        verify(workspaceRepository).deleteById(1L);
    }
}
