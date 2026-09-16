package rw.terimbere.csams.modules.subscription.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlansResponse {

    private String currency;
    private int trialMonths;
    private List<BillingPlanQuote> plans;
}
