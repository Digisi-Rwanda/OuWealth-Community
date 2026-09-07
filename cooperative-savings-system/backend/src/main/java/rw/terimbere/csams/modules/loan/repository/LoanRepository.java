package rw.terimbere.csams.modules.loan.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.terimbere.csams.modules.loan.entity.Loan;
import rw.terimbere.csams.modules.loan.entity.LoanStatus;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Optional<Loan> findByIdAndCooperativeId(UUID id, UUID cooperativeId);

    Page<Loan> findByCooperativeId(UUID cooperativeId, Pageable pageable);

    Page<Loan> findByCooperativeIdAndStatus(UUID cooperativeId, LoanStatus status, Pageable pageable);

    Page<Loan> findByCooperativeIdAndStatusIn(
            UUID cooperativeId, Collection<LoanStatus> statuses, Pageable pageable);

    Page<Loan> findByCooperativeIdAndMemberUserId(UUID cooperativeId, UUID memberUserId, Pageable pageable);

    Page<Loan> findByCooperativeIdAndMemberUserIdAndStatus(
            UUID cooperativeId, UUID memberUserId, LoanStatus status, Pageable pageable);

    List<Loan> findByCooperativeIdAndMemberUserIdOrderByRequestDateDescCreatedAtDesc(
            UUID cooperativeId, UUID memberUserId);

    List<Loan> findTop20ByCooperativeIdAndMemberUserIdOrderByRequestDateDescCreatedAtDesc(
            UUID cooperativeId, UUID memberUserId);

    List<Loan> findByCooperativeIdAndMemberUserIdAndStatusIn(
            UUID cooperativeId, UUID memberUserId, Collection<LoanStatus> statuses);

    boolean existsByCooperativeIdAndMemberUserIdAndStatusIn(
            UUID cooperativeId, UUID memberUserId, Collection<LoanStatus> statuses);

    List<Loan> findByCooperativeIdAndMemberUserIdAndDisbursementDate(
            UUID cooperativeId, UUID memberUserId, LocalDate disbursementDate);

    @Query(
            """
            SELECT COALESCE(SUM(l.outstandingPrincipal), 0)
            FROM Loan l
            WHERE l.cooperativeId = :cooperativeId
              AND l.status IN :statuses
            """)
    BigDecimal sumOutstandingPrincipalByStatuses(
            @Param("cooperativeId") UUID cooperativeId, @Param("statuses") Collection<LoanStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(l.outstandingInterest), 0)
            FROM Loan l
            WHERE l.cooperativeId = :cooperativeId
              AND l.status IN :statuses
            """)
    BigDecimal sumOutstandingInterestByStatuses(
            @Param("cooperativeId") UUID cooperativeId, @Param("statuses") Collection<LoanStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(l.principalAmount), 0)
            FROM Loan l
            WHERE l.cooperativeId = :cooperativeId
              AND l.status IN :statuses
              AND l.principalAmount IS NOT NULL
            """)
    BigDecimal sumPrincipalByStatuses(
            @Param("cooperativeId") UUID cooperativeId, @Param("statuses") Collection<LoanStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(l.principalAmount), 0)
            FROM Loan l
            WHERE l.cooperativeId = :cooperativeId
              AND l.memberUserId = :memberUserId
              AND l.status IN :statuses
              AND l.principalAmount IS NOT NULL
            """)
    BigDecimal sumPrincipalByMemberAndStatuses(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("memberUserId") UUID memberUserId,
            @Param("statuses") Collection<LoanStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(l.outstandingPrincipal), 0)
            FROM Loan l
            WHERE l.cooperativeId = :cooperativeId
              AND l.memberUserId = :memberUserId
              AND l.status IN :statuses
            """)
    BigDecimal sumOutstandingPrincipalByMemberAndStatuses(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("memberUserId") UUID memberUserId,
            @Param("statuses") Collection<LoanStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(l.outstandingInterest), 0)
            FROM Loan l
            WHERE l.cooperativeId = :cooperativeId
              AND l.memberUserId = :memberUserId
              AND l.status IN :statuses
            """)
    BigDecimal sumOutstandingInterestByMemberAndStatuses(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("memberUserId") UUID memberUserId,
            @Param("statuses") Collection<LoanStatus> statuses);

    @Query(
            """
            SELECT COALESCE(SUM(l.totalRepaidPrincipal + l.totalRepaidInterest), 0)
            FROM Loan l
            WHERE l.cooperativeId = :cooperativeId
              AND l.memberUserId = :memberUserId
            """)
    BigDecimal sumTotalRepaidByMember(
            @Param("cooperativeId") UUID cooperativeId, @Param("memberUserId") UUID memberUserId);

    List<Loan> findByStatusIn(Collection<LoanStatus> statuses);

    long countByCooperativeIdAndStatus(UUID cooperativeId, LoanStatus status);

    long countByStatus(LoanStatus status);

    long countByCooperativeIdInAndStatus(Collection<UUID> cooperativeIds, LoanStatus status);

    long countByStatusAndFirstApprovedByNot(LoanStatus status, UUID firstApprovedBy);

    long countByCooperativeIdInAndStatusAndFirstApprovedByNot(
            Collection<UUID> cooperativeIds, LoanStatus status, UUID firstApprovedBy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE Loan l
            SET l.status = rw.terimbere.csams.modules.loan.entity.LoanStatus.OVERDUE
            WHERE l.cooperativeId = :cooperativeId
              AND l.status = rw.terimbere.csams.modules.loan.entity.LoanStatus.ACTIVE
              AND l.dueDate IS NOT NULL
              AND l.dueDate < :today
              AND (l.outstandingPrincipal + l.outstandingInterest) > 0
            """)
    int markOverdue(@Param("cooperativeId") UUID cooperativeId, @Param("today") LocalDate today);
}
