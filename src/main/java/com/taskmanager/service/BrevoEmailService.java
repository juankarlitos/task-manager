package com.taskmanager.service;

import com.taskmanager.model.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BrevoEmailService implements EmailService {

    private static final DateTimeFormatter FECHA_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");

    @Value("${app.brevo.api-key:}")
    private String apiKey;

    @Value("${app.brevo.sender-email:}")
    private String senderEmail;

    @Value("${app.brevo.sender-name:Task Manager}")
    private String senderName;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private final RestClient restClient = RestClient.create();

    @Override
    public void sendReminderEmail(Task task) {
        String subject = "Recordatorio: " + task.getTitle();
        String ownerEmail = task.getOwner().getEmail();
        String ownerName  = task.getOwner().getUsername();
        String html = buildHtml(task);

        if (apiKey == null || apiKey.isBlank()) {
            log.info("[REMINDER - DRY RUN] Para: {} | Asunto: {} | Cuerpo:\n{}",
                    ownerEmail, subject, html);
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sender", Map.of("name", senderName, "email", senderEmail));
        body.put("to", List.of(Map.of("email", ownerEmail, "name", ownerName)));
        body.put("subject", subject);
        body.put("htmlContent", html);

        restClient.post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Recordatorio enviado a {} para tarea '{}'", ownerEmail, task.getTitle());
    }

    private String buildHtml(Task task) {
        String fechaStr = task.getDueDate() != null
                ? task.getDueDate().atZone(SANTIAGO).format(FECHA_FMT)
                : "Sin fecha";
        String desc = task.getDescription() != null ? htmlEscape(task.getDescription()) : "&mdash;";

        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"></head>"
             + "<body style=\"font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;color:#333\">"
             + "<h2 style=\"color:#1976d2;border-bottom:2px solid #1976d2;padding-bottom:8px\">"
             + "Recordatorio de tarea</h2>"
             + "<table style=\"width:100%;border-collapse:collapse;margin-top:12px\">"
             + row("Título", htmlEscape(task.getTitle()), "#f5f5f5")
             + row("Descripción", desc, "white")
             + row("Prioridad", task.getPriority().name(), "#f5f5f5")
             + "<tr><td style=\"padding:10px;font-weight:bold\">Fecha l&iacute;mite</td>"
             + "<td style=\"padding:10px;color:#d32f2f;font-weight:bold\">" + fechaStr + "</td></tr>"
             + "</table>"
             + "<div style=\"margin-top:24px;text-align:center\">"
             + "<a href=\"" + frontendUrl + "\" style=\"background:#1976d2;color:white;"
             + "padding:12px 24px;border-radius:4px;text-decoration:none;font-weight:bold\">"
             + "Ver en la aplicaci&oacute;n</a></div>"
             + "<p style=\"margin-top:24px;font-size:12px;color:#999;text-align:center\">"
             + "Task Manager &mdash; recordatorio autom&aacute;tico</p>"
             + "</body></html>";
    }

    private static String row(String label, String value, String bg) {
        return "<tr style=\"background:" + bg + "\">"
             + "<td style=\"padding:10px;font-weight:bold\">" + label + "</td>"
             + "<td style=\"padding:10px\">" + value + "</td></tr>";
    }

    private static String htmlEscape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
