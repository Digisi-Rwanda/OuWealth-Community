package rw.terimbere.csams.modules.loanrepayment.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
}
