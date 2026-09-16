package rw.terimbere.csams.modules.subscription.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionBillingCycle;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPayment;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentChannel;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentStatus;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, UUID> {

    Page<SubscriptionPayment> findByCooperativeId(UUID cooperativeId, Pageable pageable);

    List<SubscriptionPayment> findByCooperativeIdOrderByCreatedAtDesc(UUID cooperativeId);

    List<SubscriptionPayment> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    Optional<SubscriptionPayment> findByIdempotencyKey(String idempotencyKey);

    Optional<SubscriptionPayment> findByProviderAndExternalReference(String provider, String externalReference);

    Optional<SubscriptionPayment> findByIdAndCooperativeId(UUID id, UUID cooperativeId);

    Optional<SubscriptionPayment>
            findFirstByCooperativeIdAndBillingCycleAndPaymentChannelAndStatusOrderByInitiatedAtDesc(
                    UUID cooperativeId,
                    SubscriptionBillingCycle billingCycle,
                    SubscriptionPaymentChannel paymentChannel,
                    SubscriptionPaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select p from SubscriptionPayment p where p.id = :id")
    Optional<SubscriptionPayment> findByIdForUpdate(@Param("id") UUID id);
}
