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

    /**
     * Period totals by type for APPROVED entries only (inclusive transactionDate range).
     * Columns: transactionType, creditSum, debitSum.
     */
    @Query(
            """
            SELECT e.transactionType,
                   COALESCE(SUM(e.creditAmount), 0),
                   COALESCE(SUM(e.debitAmount), 0)
            FROM LedgerEntry e
            WHERE e.cooperativeId = :cooperativeId
              AND e.status = rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus.APPROVED
              AND e.transactionDate >= :fromDate
              AND e.transactionDate <= :toDate
              AND e.transactionType NOT IN :excludedTypes
            GROUP BY e.transactionType
            ORDER BY e.transactionType
            """)
    List<Object[]> sumApprovedCreditsAndDebitsByTypeInPeriod(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("excludedTypes") Collection<LedgerTransactionType> excludedTypes);

    @Query(
            """
            SELECT COALESCE(SUM(e.creditAmount), 0)
            FROM LedgerEntry e
            WHERE e.cooperativeId = :cooperativeId
              AND e.status = rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus.APPROVED
              AND e.transactionDate >= :fromDate
              AND e.transactionDate <= :toDate
              AND e.transactionType IN :types
            """)
    BigDecimal sumApprovedCreditsInPeriod(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("types") Collection<LedgerTransactionType> types);

    @Query(
            """
            SELECT COALESCE(SUM(e.debitAmount), 0)
            FROM LedgerEntry e
            WHERE e.cooperativeId = :cooperativeId
              AND e.status = rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus.APPROVED
              AND e.transactionDate >= :fromDate
              AND e.transactionDate <= :toDate
              AND e.transactionType IN :types
            """)
    BigDecimal sumApprovedDebitsInPeriod(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("types") Collection<LedgerTransactionType> types);

    /** Approved credits − debits for main-fund types with transactionDate strictly before {@code beforeDate}. */
    @Query(
            """
            SELECT COALESCE(SUM(e.creditAmount), 0) - COALESCE(SUM(e.debitAmount), 0)
            FROM LedgerEntry e
            WHERE e.cooperativeId = :cooperativeId
              AND e.status = rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus.APPROVED
              AND e.transactionDate < :beforeDate
              AND e.transactionType NOT IN :excludedTypes
            """)
    BigDecimal sumApprovedNetBefore(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("beforeDate") LocalDate beforeDate,
            @Param("excludedTypes") Collection<LedgerTransactionType> excludedTypes);

    /** Approved credits − debits for main-fund types with transactionDate on or before {@code throughDate}. */
    @Query(
            """
            SELECT COALESCE(SUM(e.creditAmount), 0) - COALESCE(SUM(e.debitAmount), 0)
            FROM LedgerEntry e
            WHERE e.cooperativeId = :cooperativeId
              AND e.status = rw.terimbere.csams.modules.ledger.entity.LedgerEntryStatus.APPROVED
              AND e.transactionDate <= :throughDate
              AND e.transactionType NOT IN :excludedTypes
            """)
    BigDecimal sumApprovedNetThrough(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("throughDate") LocalDate throughDate,
            @Param("excludedTypes") Collection<LedgerTransactionType> excludedTypes);
}
