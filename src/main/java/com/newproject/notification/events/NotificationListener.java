package com.newproject.notification.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    @KafkaListener(topics = "#{'${notification.topics:order.events,payment.events,shipping.events}'.split(',')}")
    public void onEvent(String payload) {
        logger.info("Received notification event: {}", payload);
    }
}
