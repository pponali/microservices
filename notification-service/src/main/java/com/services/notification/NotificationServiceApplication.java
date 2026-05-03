package com.services.notification;

import com.services.notification.webhook.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.Map;

@SpringBootApplication
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    /**
     * Bridges incoming Kafka {@code OrderPlacedEvent}s to outbound webhook deliveries.
     *
     * <p>The Kafka consumer is the inbound side (an internal event bus). The webhook
     * delivery service is the outbound side (HTTP POST to customer URLs). This listener
     * is the bridge.</p>
     */
    @Component
    @RequiredArgsConstructor
    @Slf4j
    static class OrderPlacedListener {

        private final WebhookDeliveryService webhookDeliveryService;

        @KafkaListener(topics = "notificationTopic")
        public void handleNotification(OrderPlacedEvent event) {
            log.info("Received notification for order {} — fanning out webhooks", event.getOrderNumber());
            // Fire async webhook delivery to all subscribers of "order.placed".
            webhookDeliveryService.publish("order.placed",
                    Map.of("orderNumber", event.getOrderNumber()));
        }
    }
}
