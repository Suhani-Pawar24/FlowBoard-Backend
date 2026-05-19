package com.flowboard.notification_service.service.impl;

import com.flowboard.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendTaskAssignmentEmail(String to, String taskName, String assignedBy) {
        String subject = "You have been assigned a new task: " + taskName;
        String text = String.format("Hello,\n\nYou have been assigned to the task '%s' by %s.\n\nPlease check your board for more details.\n\nBest,\nFlowBoard Team", taskName, assignedBy);
        sendSimpleEmail(to, subject, text);
    }

    @Override
    public void sendDueDateReminderEmail(String to, String taskName, String dueDate) {
        String subject = "Reminder: Task Due Soon - " + taskName;
        String text = String.format("Hello,\n\nThis is a reminder that the task '%s' is due on %s.\n\nPlease ensure it is completed on time.\n\nBest,\nFlowBoard Team", taskName, dueDate);
        sendSimpleEmail(to, subject, text);
    }

    @Override
    public void sendWorkspaceInvitationEmail(String to, String workspaceName, String invitedBy) {
        String subject = "You're invited to join a workspace: " + workspaceName;
        String text = String.format("Hello,\n\n%s has invited you to join the workspace '%s' on FlowBoard.\n\nLog in to your account to accept the invitation and start collaborating!\n\nBest,\nFlowBoard Team", invitedBy, workspaceName);
        sendSimpleEmail(to, subject, text);
    }

    @Override
    public void sendBoardInvitationEmail(String to, String boardName, String invitedBy) {
        String subject = "You've been invited to a board: " + boardName;
        String text = String.format("Hello,\n\n%s has invited you to collaborate on the board '%s' on FlowBoard.\n\nLog in to your account to start collaborating!\n\nBest,\nFlowBoard Team", invitedBy, boardName);
        sendSimpleEmail(to, subject, text);
    }

    private void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("[MAIL SUCCESS] Email sent to: {} | Subject: {}", to, subject);
        } catch (MailException e) {
            log.error("[MAIL FAILURE] Failed to send email to: {} | Reason: {}", to, e.getMessage());
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}
