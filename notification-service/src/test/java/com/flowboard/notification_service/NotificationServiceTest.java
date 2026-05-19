package com.flowboard.notification_service;

import com.flowboard.notification_service.entity.Notification;
import com.flowboard.notification_service.entity.NotificationType;
import com.flowboard.notification_service.repository.NotificationRepository;
import com.flowboard.notification_service.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setNotificationId(1L);
        notification.setRecipientId(100L);
        notification.setType(NotificationType.ASSIGNMENT);
        notification.setMessage("New task assigned");
        notification.setRead(false);
    }

    @Test
    @DisplayName("NT-01: send() saves notification and pushes WebSocket message")
    void send_savesAndPushes() {
        when(notificationRepository.save(any())).thenReturn(notification);
        Notification saved = notificationService.send(new Notification());
        assertNotNull(saved);
        assertEquals(100L, saved.getRecipientId());
        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("NT-02: getByRecipient returns all notifications for user")
    void getByRecipient_returnsList() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(notification));
        List<Notification> results = notificationService.getByRecipient(100L);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("NT-03: markAsRead sets read flag to true")
    void markAsRead_updatesStatus() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Notification result = notificationService.markAsRead(1L);
        assertTrue(result.isRead());
    }

    @Test
    @DisplayName("NT-04: markAllRead updates all unread notifications for a recipient")
    void markAllRead_updatesAll() {
        Notification n2 = new Notification();
        n2.setRead(false);
        when(notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(100L, false))
                .thenReturn(List.of(notification, n2));

        notificationService.markAllRead(100L);
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("NT-05: getUnreadCount returns correct count")
    void getUnreadCount_returnsValue() {
        when(notificationRepository.countByRecipientIdAndIsRead(100L, false)).thenReturn(5L);
        assertEquals(5L, notificationService.getUnreadCount(100L));
    }
}
