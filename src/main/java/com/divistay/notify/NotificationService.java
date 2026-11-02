package com.divistay.notify;

import java.util.List;

/** Sends messages to members and organizers. Swap the implementation for real email. */
public interface NotificationService {

    void send(Notification notification);

    /** Messages sent for a group, newest first (used by the demo outbox). */
    List<Notification> sentFor(String groupId);
}
