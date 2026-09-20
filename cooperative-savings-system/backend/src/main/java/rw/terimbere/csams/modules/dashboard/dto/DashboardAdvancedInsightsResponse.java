package rw.terimbere.csams.modules.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.terimbere.csams.modules.investment.entity.InvestmentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAdvancedInsightsResponse {

    private Period period;
    private String currency;
    private String timezone;

    @Builder.Default
    private List<LargestActiveInvestmentRow> largestActiveInvestments = new ArrayList<>();

    @Builder.Default
    private List<FrequentBorrowerRow> frequentBorrowers = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Period {
        private int year;
        private LocalDate asOf;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LargestActiveInvestmentRow {
        private UUID investmentId;
        private String name;
        private BigDecimal originalCapital;
        private BigDecimal remainingCapital;
        private BigDecimal profitReturned;
        private InvestmentStatus status;
        private int rank;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FrequentBorrowerRow {
        private UUID memberId;
        private String displayName;
        private long numberOfLoansDisbursed;
        private BigDecimal totalPrincipalBorrowed;
        private int rank;
    }
}
