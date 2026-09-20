package rw.terimbere.csams.modules.report.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.terimbere.csams.modules.ledger.repository.LedgerEntryRepository;
import rw.terimbere.csams.shared.financial.LedgerTransactionType;
import rw.terimbere.csams.shared.utilities.MoneyUtils;

/**
 * Period-scoped APPROVED ledger aggregation for Full Financial statements.
 *
 * <p>Uses {@code transactionDate} (not createdAt). Social fund types are excluded from main-fund
 * balances and period type maps.
 */
@Service
@RequiredArgsConstructor
public class LedgerPeriodAggregationService {

    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional(readOnly = true)
    public LedgerPeriodTotals aggregateMainFund(UUID cooperativeId, LocalDate fromDate, LocalDate toDate) {
        if (cooperativeId == null || fromDate == null || toDate == null) {
            return LedgerPeriodTotals.empty();
        }
        EnumSet<LedgerTransactionType> excluded =
                EnumSet.copyOf(FullFinancialLedgerClassifications.SOCIAL_FUND_TYPES);

        Map<LedgerTransactionType, BigDecimal> credits = LedgerPeriodTotals.newTypeMap();
        Map<LedgerTransactionType, BigDecimal> debits = LedgerPeriodTotals.newTypeMap();
        BigDecimal periodCredits = BigDecimal.ZERO;
        BigDecimal periodDebits = BigDecimal.ZERO;

        List<Object[]> rows = ledgerEntryRepository.sumApprovedCreditsAndDebitsByTypeInPeriod(
                cooperativeId, fromDate, toDate, excluded);
        for (Object[] row : rows) {
            LedgerTransactionType type = (LedgerTransactionType) row[0];
            BigDecimal credit = MoneyUtils.scale(nvl((BigDecimal) row[1]));
            BigDecimal debit = MoneyUtils.scale(nvl((BigDecimal) row[2]));
            credits.put(type, credit);
            debits.put(type, debit);
            periodCredits = MoneyUtils.add(periodCredits, credit);
            periodDebits = MoneyUtils.add(periodDebits, debit);
        }

        BigDecimal opening = MoneyUtils.scale(nvl(ledgerEntryRepository.sumApprovedNetBefore(
                cooperativeId, fromDate, excluded)));
        BigDecimal closing = MoneyUtils.scale(nvl(ledgerEntryRepository.sumApprovedNetThrough(
                cooperativeId, toDate, excluded)));

        return LedgerPeriodTotals.builder()
                .creditsByType(Map.copyOf(credits))
                .debitsByType(Map.copyOf(debits))
                .openingLedgerBalance(opening)
                .closingLedgerBalance(closing)
                .periodCredits(MoneyUtils.scale(periodCredits))
                .periodDebits(MoneyUtils.scale(periodDebits))
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal sumApprovedCreditsInPeriod(
            UUID cooperativeId, LocalDate from, LocalDate to, EnumSet<LedgerTransactionType> types) {
        return MoneyUtils.scale(nvl(ledgerEntryRepository.sumApprovedCreditsInPeriod(cooperativeId, from, to, types)));
    }

    @Transactional(readOnly = true)
    public BigDecimal sumApprovedDebitsInPeriod(
            UUID cooperativeId, LocalDate from, LocalDate to, EnumSet<LedgerTransactionType> types) {
        return MoneyUtils.scale(nvl(ledgerEntryRepository.sumApprovedDebitsInPeriod(cooperativeId, from, to, types)));
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
