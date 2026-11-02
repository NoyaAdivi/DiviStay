package com.divistay.notify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Keeps sent messages in memory and logs them, instead of sending real email.
 * The organizer dashboard shows this outbox, so the whole flow can be tried in a browser.
 */
public class OutboxNotificationService implements NotificationService {

    private static final Logger LOG = Logger.getLogger(OutboxNotificationService.class.getName());
    private final List<Notification> outbox = new CopyOnWriteArrayList<>();

    @Override
    public void send(Notification notification) {
        outbox.add(notification);
        LOG.info(() -> "To " + notification.to() + ": " + notification.subject());
    }

    @Override
    public List<Notification> sentFor(String groupId) {
        List<Notification> result = new ArrayList<>();
        for (Notification n : outbox) {
            if (n.groupId().equals(groupId)) {
                result.add(n);
            }
        }
        result.sort(Comparator.comparing(Notification::sentAt).reversed());
        return result;
    }
}
