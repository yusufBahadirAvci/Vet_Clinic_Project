package com.vetclinic.notification;

import com.vetclinic.notification.domain.Channel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Looks up the sender for a channel. A new channel is a new {@link NotificationSender} bean, nothing else. */
@Component
public class NotificationSenderFactory {

    private final Map<Channel, NotificationSender> senders = new EnumMap<>(Channel.class);

    public NotificationSenderFactory(List<NotificationSender> availableSenders) {
        for (NotificationSender sender : availableSenders) {
            NotificationSender previous = senders.put(sender.channel(), sender);
            if (previous != null) {
                throw new IllegalStateException("More than one sender registered for channel " + sender.channel());
            }
        }
    }

    public NotificationSender get(Channel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No sender registered for channel " + channel);
        }
        return sender;
    }
}
