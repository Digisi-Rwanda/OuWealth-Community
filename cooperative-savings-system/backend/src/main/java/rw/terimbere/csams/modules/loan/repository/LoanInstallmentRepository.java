package rw.terimbere.csams.modules.loan.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import rw.terimbere.csams.modules.loan.entity.LoanInstallment;
import rw.terimbere.csams.modules.loan.entity.LoanInstallmentStatus;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, UUID> {

    List<LoanInstallment> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    boolean existsByLoanId(UUID loanId);

    List<LoanInstallment> findByStatusInAndDueDateLessThanEqual(
            Collection<LoanInstallmentStatus> statuses, LocalDate asOf);
}
