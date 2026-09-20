package rw.terimbere.csams.modules.loanrepayment.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rw.terimbere.csams.modules.loanrepayment.entity.LoanRepayment;

public interface LoanRepaymentRepository
        extends JpaRepository<LoanRepayment, UUID>, JpaSpecificationExecutor<LoanRepayment> {

    List<LoanRepayment> findByLoanIdAndCooperativeIdOrderByPaymentDateDescCreatedAtDesc(
            UUID loanId, UUID cooperativeId);

    List<LoanRepayment> findByLoanIdOrderByPaymentDateAscCreatedAtAsc(UUID loanId);

    List<LoanRepayment> findByCooperativeIdAndLoanIdAndPaymentDateAndAmountTotal(
            UUID cooperativeId, UUID loanId, LocalDate paymentDate, BigDecimal amountTotal);

    default List<LoanRepayment> findFiltered(
            UUID cooperativeId, UUID memberUserId, LocalDate fromDate, LocalDate toDate) {
        return findAll(
                LoanRepaymentSpecs.filtered(cooperativeId, memberUserId, fromDate, toDate),
                Sort.by(Sort.Direction.DESC, "paymentDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Query(
            """
            SELECT COALESCE(SUM(r.amountTotal), 0)
            FROM LoanRepayment r
            WHERE r.cooperativeId = :cooperativeId
              AND r.paymentDate >= :fromDate
              AND r.paymentDate <= :toDate
            """)
    BigDecimal sumAmountTotalInDateRange(
            @Param("cooperativeId") UUID cooperativeId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
