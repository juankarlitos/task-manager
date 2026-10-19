package com.taskmanager.service;

import com.taskmanager.model.Task;
import com.taskmanager.model.User;

public interface EmailService {
    void sendReminderEmail(Task task);
    void sendPasswordResetEmail(User user, String rawToken);
    void sendPasswordChangedEmail(User user);
}
