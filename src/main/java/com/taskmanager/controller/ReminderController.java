package com.taskmanager.controller;

import com.taskmanager.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
public class ReminderController {

    private final ReminderService reminderService;

    @Value("${app.cron-secret:}")
    private String cronSecret;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run(
            @RequestHeader(value = "X-Cron-Secret", required = false) String secret) {

        if (cronSecret == null || cronSecret.isBlank() || !cronSecret.equals(secret)) {
            log.warn("Intento de ejecutar recordatorios con secreto inválido o ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int sent = reminderService.sendReminders();
        return ResponseEntity.ok(Map.of("sent", sent));
    }
}
