package rw.terimbere.csams.modules.fine.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.terimbere.csams.modules.fine.entity.Fine;
import rw.terimbere.csams.modules.fine.entity.FineStatus;

public interface FineRepository extends JpaRepository<Fine, UUID> {

    Optional<Fine> findByIdAndCooperativeId(UUID id, UUID cooperativeId);

    Page<Fine> findByCooperativeId(UUID cooperativeId, Pageable pageable);

    Page<Fine> findByCooperativeIdAndStatus(UUID cooperativeId, FineStatus status, Pageable pageable);

    Page<Fine> findByCooperativeIdAndMemberUserId(UUID cooperativeId, UUID memberUserId, Pageable pageable);

    Page<Fine> findByCooperativeIdAndMemberUserIdAndStatus(
            UUID cooperativeId, UUID memberUserId, FineStatus status, Pageable pageable);

    List<Fine> findByCooperativeIdAndMemberUserIdOrderByIssuedDateDescCreatedAtDesc(
            UUID cooperativeId, UUID memberUserId);

    List<Fine> findByCooperativeIdAndMemberUserIdAndIssuedDate(
            UUID cooperativeId, UUID memberUserId, java.time.LocalDate issuedDate);

    List<Fine> findTop20ByCooperativeIdAndMemberUserIdOrderByIssuedDateDescCreatedAtDesc(
            UUID cooperativeId, UUID memberUserId);

    boolean existsByCooperativeIdAndSourceContributionId(UUID cooperativeId, UUID sourceContributionId);

    boolean existsByAutomaticSourceKey(String automaticSourceKey);

    long countByCooperativeId(UUID cooperativeId);

    long countByCooperativeIdAndStatusIn(UUID cooperativeId, Collection<FineStatus> statuses);

    @Query(
            """
            SELECT COUNT(DISTINCT f.memberUserId)
            FROM Fine f
            WHERE f.cooperativeId = :cooperativeId
              AND f.status IN :statuses
            """)
    long countDistinctMembersByStatusIn(
            @Param("cooperativeId") UUID cooperativeId, @Param("statuses") Collection<FineStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(f.totalAmount), 0)
            FROM Fine f
            WHERE f.cooperativeId = :cooperativeId
              AND f.status NOT IN (
                  rw.terimbere.csams.modules.fine.entity.FineStatus.CANCELLED
              )
            """)
    BigDecimal sumTotalAmountExcludingCancelled(@Param("cooperativeId") UUID cooperativeId);

    @Query(
            """
            SELECT COALESCE(SUM(f.totalAmount), 0)
            FROM Fine f
            WHERE f.cooperativeId = :cooperativeId
              AND f.memberUserId = :memberUserId
              AND f.status NOT IN (
                  rw.terimbere.csams.modules.fine.entity.FineStatus.CANCELLED
              )
            """)
    BigDecimal sumTotalAmountByMemberExcludingCancelled(
            @Param("cooperativeId") UUID cooperativeId, @Param("memberUserId") UUID memberUserId);

    @Query(
            """
            SELECT COALESCE(SUM(f.outstandingAmount), 0)
            FROM Fine f
            WHERE f.cooperativeId = :cooperativeId
              AND f.memberUserId = :memberUserId
              AND f.status IN :statuses
            """)
    BigDecimal sumOutstandingByMemberAndStatuses(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("memberUserId") UUID memberUserId,
            @Param("statuses") Collection<FineStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(f.outstandingAmount), 0)
            FROM Fine f
            WHERE f.cooperativeId = :cooperativeId
              AND f.status IN :statuses
            """)
    BigDecimal sumOutstandingByStatuses(
            @Param("cooperativeId") UUID cooperativeId, @Param("statuses") Collection<FineStatus> statuses);
}
