package com.taskmanager.service;

import com.taskmanager.model.Task;

public interface EmailService {
    void sendReminderEmail(Task task);
}
