package com.services.notification.webhook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {

    List<WebhookSubscription> findByEventTypeAndEnabledTrue(String eventType);

    Page<WebhookSubscription> findAll(Pageable pageable);
}
