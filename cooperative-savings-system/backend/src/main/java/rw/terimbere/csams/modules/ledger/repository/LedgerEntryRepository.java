package rw.terimbere.csams.modules.ledger.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.terimbere.csams.modules.ledger.entity.LedgerEntry;
import rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus;
import rw.terimbere.csams.shared.financial.LedgerTransactionType;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, UUID>, JpaSpecificationExecutor<LedgerEntry> {

    long countByCooperativeId(UUID cooperativeId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerEntry> findByIdAndCooperativeId(UUID id, UUID cooperativeId);

    Optional<LedgerEntry> findFirstBySourceEntityTypeAndSourceEntityIdAndTransactionTypeAndStatusOrderByCreatedAtDesc(
            String sourceEntityType,
            UUID sourceEntityId,
            LedgerTransactionType transactionType,
            LedgerEntryStatus status);

    List<LedgerEntry> findBySourceEntityTypeAndSourceEntityIdAndStatus(
            String sourceEntityType, UUID sourceEntityId, LedgerEntryStatus status);

    default Page<LedgerEntry> findFiltered(
            UUID cooperativeId,
            LedgerTransactionType transactionType,
            LocalDate fromDate,
            LocalDate toDate,
            UUID memberUserId,
            String sourceEntityType,
            Pageable pageable) {
        Pageable page = pageable == null ? Pageable.unpaged() : pageable;
        return findAll(
                LedgerEntrySpecs.filtered(
                        cooperativeId, transactionType, fromDate, toDate, memberUserId, sourceEntityType),
                page);
    }

    @Query(
            """
            SELECT COALESCE(SUM(e.creditAmount), 0)
            FROM LedgerEntry e
            WHERE e.cooperativeId = :cooperativeId
              AND e.status = rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus.APPROVED
              AND e.transactionType IN :types
            """)
    BigDecimal sumApprovedCredits(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("types") Collection<LedgerTransactionType> types);

    @Query(
            """
            SELECT COALESCE(SUM(e.debitAmount), 0)
            FROM LedgerEntry e
            WHERE e.cooperativeId = :cooperativeId
              AND e.status = rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus.APPROVED
              AND e.transactionType IN :types
            """)
    BigDecimal sumApprovedDebits(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("types") Collection<LedgerTransactionType> types);
}
