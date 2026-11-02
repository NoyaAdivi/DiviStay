package com.divistay.notify;

import java.time.Instant;

/** A message to a person (email in production; shown in the demo outbox here). */
public record Notification(String groupId, String to, String subject, String body, String link, Instant sentAt) {
}
