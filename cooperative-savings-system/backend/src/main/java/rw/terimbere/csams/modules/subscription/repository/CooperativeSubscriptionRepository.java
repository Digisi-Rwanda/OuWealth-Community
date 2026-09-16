package rw.terimbere.csams.modules.subscription.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.terimbere.csams.modules.subscription.entity.CooperativeSubscription;

public interface CooperativeSubscriptionRepository extends JpaRepository<CooperativeSubscription, UUID> {

    Optional<CooperativeSubscription> findByCooperativeId(UUID cooperativeId);

    boolean existsByCooperativeId(UUID cooperativeId);

    long countByCooperativeId(UUID cooperativeId);
}
