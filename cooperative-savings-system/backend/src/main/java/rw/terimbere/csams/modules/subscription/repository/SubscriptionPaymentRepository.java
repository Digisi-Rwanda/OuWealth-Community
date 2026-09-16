package rw.terimbere.csams.modules.subscription.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPayment;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, UUID> {

    Page<SubscriptionPayment> findByCooperativeId(UUID cooperativeId, Pageable pageable);

    List<SubscriptionPayment> findByCooperativeIdOrderByCreatedAtDesc(UUID cooperativeId);

    List<SubscriptionPayment> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    Optional<SubscriptionPayment> findByIdempotencyKey(String idempotencyKey);

    Optional<SubscriptionPayment> findByProviderAndExternalReference(String provider, String externalReference);
}
