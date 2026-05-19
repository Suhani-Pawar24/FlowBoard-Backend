package com.flowboard.notification_service.service;

public interface EmailService {
    void sendTaskAssignmentEmail(String to, String taskName, String assignedBy);
    void sendDueDateReminderEmail(String to, String taskName, String dueDate);
    void sendWorkspaceInvitationEmail(String to, String workspaceName, String invitedBy);
    void sendBoardInvitationEmail(String to, String boardName, String invitedBy);
}
