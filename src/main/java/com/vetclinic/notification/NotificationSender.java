package com.vetclinic.notification;

import com.vetclinic.notification.domain.Channel;

/** Strategy: one implementation per delivery channel. */
public interface NotificationSender {

    Channel channel();

    /** @param destination normalized e-mail address or phone number */
    void send(String destination, String subject, String body);
}
