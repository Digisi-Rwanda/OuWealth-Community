package rw.terimbere.csams.modules.contribution.repository;

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
import rw.terimbere.csams.modules.contribution.entity.Contribution;
import rw.terimbere.csams.modules.contribution.entity.ContributionReviewStatus;
import rw.terimbere.csams.modules.contribution.entity.ContributionStatus;

public interface ContributionRepository
        extends JpaRepository<Contribution, UUID>, JpaSpecificationExecutor<Contribution> {

    List<Contribution> findByCooperativeIdAndYearAndMonth(UUID cooperativeId, int year, int month);

    List<Contribution> findByCooperativeIdAndStatusIn(
            UUID cooperativeId, Collection<ContributionStatus> statuses);

    List<Contribution> findByCooperativeIdAndYearAndMonthAndStatusIn(
            UUID cooperativeId, int year, int month, Collection<ContributionStatus> statuses);

    Optional<Contribution> findByCooperativeIdAndMemberUserIdAndYearAndMonth(
            UUID cooperativeId, UUID memberUserId, int year, int month);

    boolean existsByCooperativeIdAndMemberUserIdAndYearAndMonth(
            UUID cooperativeId, UUID memberUserId, int year, int month);

    List<Contribution> findByCooperativeIdAndMemberUserIdOrderByYearDescMonthDesc(
            UUID cooperativeId, UUID memberUserId);

    List<Contribution> findTop20ByCooperativeIdAndMemberUserIdOrderByYearDescMonthDesc(
            UUID cooperativeId, UUID memberUserId);

    Optional<Contribution> findByIdAndCooperativeId(UUID id, UUID cooperativeId);

    Page<Contribution> findByCooperativeIdAndReviewStatus(
            UUID cooperativeId, ContributionReviewStatus reviewStatus, Pageable pageable);

    long countByReviewStatus(ContributionReviewStatus reviewStatus);

    long countByCooperativeIdInAndReviewStatus(
            Collection<UUID> cooperativeIds, ContributionReviewStatus reviewStatus);

    default Page<Contribution> search(
            UUID cooperativeId,
            UUID memberUserId,
            Integer year,
            Integer month,
            ContributionStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        Pageable page = pageable == null ? Pageable.unpaged() : pageable;
        return findAll(
                ContributionSpecs.filtered(
                        cooperativeId, memberUserId, year, month, status, fromDate, toDate),
                page);
    }

    @Query(
            """
            SELECT COALESCE(SUM(c.paidAmount), 0)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.status IN (
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PAID,
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PARTIALLY_PAID
              )
              AND (:year IS NULL OR c.year = :year)
              AND (:month IS NULL OR c.month = :month)
            """)
    BigDecimal sumPaidForPeriod(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("year") Integer year,
            @Param("month") Integer month);

    @Query(
            """
            SELECT c.month, COALESCE(SUM(c.paidAmount), 0)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.year = :year
              AND c.status IN (
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PAID,
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PARTIALLY_PAID
              )
            GROUP BY c.month
            ORDER BY c.month
            """)
    List<Object[]> sumPaidByMonth(@Param("cooperativeId") UUID cooperativeId, @Param("year") int year);

    @Query(
            """
            SELECT c.memberUserId, COALESCE(SUM(c.paidAmount), 0)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.status IN (
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PAID,
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PARTIALLY_PAID
              )
              AND c.paymentDate IS NOT NULL
              AND c.paymentDate >= :fromDate
              AND c.paymentDate <= :toDate
            GROUP BY c.memberUserId
            """)
    List<Object[]> sumPaidGroupedByMemberInDateRange(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(
            """
            SELECT c.memberUserId, COALESCE(SUM(c.paidAmount), 0)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.status IN (
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PAID,
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PARTIALLY_PAID
              )
              AND c.paymentDate IS NOT NULL
              AND c.paymentDate >= :fromDate
              AND c.paymentDate <= :toDate
            GROUP BY c.memberUserId
            ORDER BY COALESCE(SUM(c.paidAmount), 0) DESC
            """)
    List<Object[]> sumPaidGroupedByMemberInDateRangeOrdered(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query(
            """
            SELECT COALESCE(SUM(c.paidAmount), 0)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.memberUserId = :memberUserId
            """)
    BigDecimal sumPaidByMember(
            @Param("cooperativeId") UUID cooperativeId, @Param("memberUserId") UUID memberUserId);

    @Query(
            """
            SELECT COALESCE(SUM(c.expectedAmount), 0)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.memberUserId = :memberUserId
              AND c.status NOT IN (
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.WAIVED,
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.CANCELLED
              )
            """)
    BigDecimal sumExpectedByMember(
            @Param("cooperativeId") UUID cooperativeId, @Param("memberUserId") UUID memberUserId);

    @Query(
            """
            SELECT COALESCE(SUM(c.outstandingAmount), 0)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.memberUserId = :memberUserId
              AND c.status IN (
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PENDING,
                  rw.terimbere.csams.modules.contribution.entity.ContributionStatus.PARTIALLY_PAID
              )
            """)
    BigDecimal sumOutstandingByMember(
            @Param("cooperativeId") UUID cooperativeId, @Param("memberUserId") UUID memberUserId);

    /**
     * FULL_FINANCIAL multi-month aggregation by obligation year/month (not paymentDate).
     *
     * <p>Excludes {@code CANCELLED}. Waived rows contribute 0 expected (domain treats waived as
     * non-obligatory) while paid amounts and zero outstanding are retained.
     *
     * <p>Columns: memberUserId, expected, paid, remaining, overpaid, periodsCounted
     */
    @Query(
            """
            SELECT c.memberUserId,
                   COALESCE(SUM(CASE
                       WHEN c.status = rw.terimbere.csams.modules.contribution.entity.ContributionStatus.WAIVED
                       THEN 0
                       ELSE COALESCE(c.expectedAmount, 0)
                   END), 0),
                   COALESCE(SUM(COALESCE(c.paidAmount, 0)), 0),
                   COALESCE(SUM(COALESCE(c.outstandingAmount, 0)), 0),
                   COALESCE(SUM(CASE
                       WHEN c.status = rw.terimbere.csams.modules.contribution.entity.ContributionStatus.WAIVED
                       THEN 0
                       WHEN COALESCE(c.paidAmount, 0) > COALESCE(c.expectedAmount, 0)
                       THEN COALESCE(c.paidAmount, 0) - COALESCE(c.expectedAmount, 0)
                       ELSE 0
                   END), 0),
                   COUNT(c)
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.status <> rw.terimbere.csams.modules.contribution.entity.ContributionStatus.CANCELLED
              AND (
                    c.year > :fromYear
                    OR (c.year = :fromYear AND c.month >= :fromMonth)
                  )
              AND (
                    c.year < :toYear
                    OR (c.year = :toYear AND c.month <= :toMonth)
                  )
            GROUP BY c.memberUserId
            ORDER BY c.memberUserId
            """)
    List<Object[]> sumAggregatesByMemberInObligationRange(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("fromYear") int fromYear,
            @Param("fromMonth") int fromMonth,
            @Param("toYear") int toYear,
            @Param("toMonth") int toMonth);

    /**
     * Single-month FULL_FINANCIAL detail rows (excludes CANCELLED).
     * Ordered by member for stable PDF output after name hydration.
     */
    @Query(
            """
            SELECT c
            FROM Contribution c
            WHERE c.cooperativeId = :cooperativeId
              AND c.year = :year
              AND c.month = :month
              AND c.status <> rw.terimbere.csams.modules.contribution.entity.ContributionStatus.CANCELLED
            ORDER BY c.memberUserId
            """)
    List<Contribution> findObligationRowsForMonth(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("year") int year,
            @Param("month") int month);
}

