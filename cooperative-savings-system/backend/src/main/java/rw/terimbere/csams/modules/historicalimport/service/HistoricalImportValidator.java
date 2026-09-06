package rw.terimbere.csams.modules.historicalimport.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rw.terimbere.csams.modules.contribution.entity.ContributionStatus;
import rw.terimbere.csams.modules.contribution.repository.ContributionRepository;
import rw.terimbere.csams.modules.contribution.service.ContributionService;
import rw.terimbere.csams.modules.fine.entity.FineStatus;
import rw.terimbere.csams.modules.fine.entity.FineType;
import rw.terimbere.csams.modules.historicalimport.dto.HistoricalImportError;
import rw.terimbere.csams.modules.historicalimport.dto.HistoricalImportSheetSummary;
import rw.terimbere.csams.modules.historicalimport.dto.HistoricalReconciliationSummary;
import rw.terimbere.csams.modules.historicalimport.dto.HistoricalYearSummary;
import rw.terimbere.csams.modules.historicalimport.entity.HistoricalImportSheet;
import rw.terimbere.csams.modules.historicalimport.repository.HistoricalImportRowRepository;
import rw.terimbere.csams.modules.incomeexpense.entity.IncomeExpenseCategory;
import rw.terimbere.csams.modules.incomeexpense.entity.LedgerEffect;
import rw.terimbere.csams.modules.investment.entity.InvestmentStatus;
import rw.terimbere.csams.modules.loan.entity.InterestType;
import rw.terimbere.csams.modules.loan.entity.LoanStatus;
import rw.terimbere.csams.modules.loan.service.LoanInterestCalculator;
import rw.terimbere.csams.modules.membership.repository.CooperativeMembershipRepository;
import rw.terimbere.csams.modules.payout.entity.PayoutLineStatus;
import rw.terimbere.csams.modules.payout.entity.PayoutRunStatus;
import rw.terimbere.csams.modules.specialcontribution.entity.SpecialCampaignStatus;
import rw.terimbere.csams.modules.user.entity.User;
import rw.terimbere.csams.modules.user.repository.UserRepository;
import rw.terimbere.csams.security.CooperativeOfficerRoles;
import rw.terimbere.csams.security.UserPrincipal;
import rw.terimbere.csams.shared.financial.FinancialCalculationService;
import rw.terimbere.csams.shared.utilities.MoneyUtils;
import rw.terimbere.csams.modules.socialfund.service.SocialFundBalanceService;

@Component
@RequiredArgsConstructor
class HistoricalImportValidator {

    private static final Set<String> MEMBERSHIP_STATUSES = Set.of("ACTIVE", "INACTIVE", "SUSPENDED");
    private static final Set<LoanStatus> HISTORICAL_LOAN_STATUSES =
            EnumSet.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE, LoanStatus.CLOSED, LoanStatus.WRITTEN_OFF);
    private static final Set<InvestmentStatus> HISTORICAL_INVESTMENT_STATUSES = EnumSet.of(
            InvestmentStatus.ACTIVE,
            InvestmentStatus.PARTIALLY_RETURNED,
            InvestmentStatus.COMPLETED,
            InvestmentStatus.CANCELLED,
            InvestmentStatus.LOSS_RECORDED);

    private final UserRepository userRepository;
    private final CooperativeMembershipRepository membershipRepository;
    private final ContributionRepository contributionRepository;
    private final HistoricalImportRowRepository importRowRepository;
    private final HistoricalOperationalLookup operationalLookup;
    private final FinancialCalculationService financialCalculationService;
    private final SocialFundBalanceService socialFundBalanceService;

    ValidatedWorkbook validate(UUID cooperativeId, ParsedWorkbook parsed, UserPrincipal actor) {
        ValidatedWorkbook out = new ValidatedWorkbook();
        out.errors.addAll(parsed.workbookErrors());
        if (!parsed.workbookErrors().isEmpty()) {
            summarizeEmptySheets(out, parsed);
            out.reconciliation = emptyReconciliation();
            return out;
        }

        Map<String, UUID> usernames = new HashMap<>();
        Map<String, UUID> campaignCodes = new HashMap<>();
        Map<String, UUID> loanCodes = new HashMap<>();
        Map<String, UUID> fineCodes = new HashMap<>();
        Map<String, UUID> investmentCodes = new HashMap<>();
        Map<String, UUID> payoutCodes = new HashMap<>();

        validateMembers(cooperativeId, parsed, actor, out, usernames);
        validateCampaigns(cooperativeId, parsed, out, campaignCodes);
        validateContributions(cooperativeId, parsed, out, usernames);
        validateSpecialContributions(cooperativeId, parsed, out, usernames, campaignCodes);
        validateSocialContributions(cooperativeId, parsed, out, usernames);
        validateLoans(cooperativeId, parsed, out, usernames, loanCodes);
        validateFines(cooperativeId, parsed, out, usernames, fineCodes);
        validateInvestments(cooperativeId, parsed, out, investmentCodes);
        validateRepayments(cooperativeId, parsed, out, usernames, loanCodes);
        validateFinePayments(cooperativeId, parsed, out, usernames, fineCodes);
        validateInvestmentReturns(cooperativeId, parsed, out, investmentCodes);
        validateIncomeExpenses(cooperativeId, parsed, out, true);
        validateIncomeExpenses(cooperativeId, parsed, out, false);
        validateSocialDisbursements(cooperativeId, parsed, out, usernames);
        validatePayouts(cooperativeId, parsed, out, payoutCodes);
        validatePayoutLines(cooperativeId, parsed, out, usernames, payoutCodes);
        reconcileLoanTotals(out);
        reconcileFineTotals(out);
        reconcilePayoutTotals(out);
        out.yearSummaries = buildYearSummaries(out);
        out.reconciliation = buildReconciliation(cooperativeId, out);
        if (out.reconciliation != null) {
            out.reconciliation.setYearSummaries(out.yearSummaries);
        }
        out.reportReady = !out.hasErrors()
                && out.validRows() > 0
                && (out.reconciliation == null || !out.reconciliation.isBlocked());
        return out;
    }

    private void validateMembers(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            UserPrincipal actor,
            ValidatedWorkbook out,
            Map<String, UUID> usernames) {
        Set<String> seenUsernames = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenNids = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.MEMBERS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String username = required(row, "Username", errors);
            String firstName = required(row, "First Name", errors);
            String lastName = required(row, "Last Name", errors);
            String email = required(row, "Email", errors);
            if (StringUtils.hasText(email)) {
                email = email.trim().toLowerCase(Locale.ROOT);
                if (!email.contains("@")) {
                    errors.add(err(row, "Email", "INVALID_EMAIL", "Email is not valid"));
                }
            }
            String phone = row.get("Phone");
            String nationalId = trim(row.get("National ID"));
            LocalDate membershipDate = requiredDate(
                    row,
                    "Membership Date",
                    errors,
                    "Membership Date is required. Do not leave it blank or use today's date unless the member actually joined today.");
            // Opening/historical owned shares, not an in-period SharePurchase.
            Integer shareCount = optionalInt(row, "Share Count", 1, errors);
            if (shareCount != null && (shareCount < 1 || shareCount > 1000)) {
                errors.add(err(row, "Share Count", "INVALID_SHARE_COUNT", "Share count must be between 1 and 1000"));
                shareCount = 1;
            }
            String status = defaulted(row.get("Membership Status"), "ACTIVE").toUpperCase(Locale.ROOT);
            if (!MEMBERSHIP_STATUSES.contains(status)) {
                errors.add(err(
                        row,
                        "Membership Status",
                        "INVALID_STATUS",
                        "Membership Status must be ACTIVE, INACTIVE, or SUSPENDED"));
            }
            String requestedRole = defaulted(row.get("Role"), "MEMBER");
            String role = "MEMBER";
            try {
                requestedRole = CooperativeOfficerRoles.normalize(requestedRole);
                role = requestedRole;
            } catch (Exception ex) {
                errors.add(err(row, "Role", "INVALID_ROLE", ex.getMessage()));
                requestedRole = "MEMBER";
                role = "MEMBER";
            }

            String usernameKey = HistoricalFingerprint.normalize(username);
            if (StringUtils.hasText(username) && !seenUsernames.add(usernameKey)) {
                errors.add(err(row, "Username", "DUPLICATE_USERNAME", "Username is duplicated in this workbook"));
            }
            if (StringUtils.hasText(email) && !seenEmails.add(email)) {
                errors.add(err(row, "Email", "DUPLICATE_EMAIL", "Email is duplicated in this workbook"));
            }
            if (nationalId != null && !seenNids.add(nationalId)) {
                errors.add(err(row, "National ID", "DUPLICATE_NATIONAL_ID", "National ID is duplicated in this workbook"));
            }

            UUID existingUserId = null;
            boolean createUser = true;
            if (errors.isEmpty() && StringUtils.hasText(username)) {
                Optional<User> byUsername = userRepository.findByUsernameIgnoreCaseAndDeletedFalse(username.trim());
                Optional<User> byEmail =
                        StringUtils.hasText(email) ? userRepository.findByEmailIgnoreCaseAndDeletedFalse(email) : Optional.empty();
                Optional<User> byNid = nationalId == null
                        ? Optional.empty()
                        : userRepository.findByNationalIdAndDeletedFalse(nationalId);
                Set<UUID> identities = new HashSet<>();
                byUsername.ifPresent(u -> identities.add(u.getId()));
                byEmail.ifPresent(u -> identities.add(u.getId()));
                byNid.ifPresent(u -> identities.add(u.getId()));
                if (identities.size() > 1) {
                    errors.add(err(
                            row,
                            "Username",
                            "IDENTITY_CONFLICT",
                            "Username, email, and national ID resolve to different existing users"));
                } else if (identities.size() == 1) {
                    User existing = byUsername.or(() -> byEmail).or(() -> byNid).orElseThrow();
                    if (byUsername.isPresent()
                            && byEmail.isPresent()
                            && !byUsername.get().getId().equals(byEmail.get().getId())) {
                        errors.add(err(row, "Email", "IDENTITY_CONFLICT", "Email belongs to a different user"));
                    } else if (byUsername.isPresent()
                            && StringUtils.hasText(email)
                            && !email.equalsIgnoreCase(existing.getEmail())) {
                        errors.add(err(
                                row,
                                "Email",
                                "IDENTITY_CONFLICT",
                                "Existing username has a different email; profile is not overwritten"));
                    } else {
                        existingUserId = existing.getId();
                        createUser = false;
                    }
                }
            }
            role = resolveImportedRole(row, actor, cooperativeId, existingUserId, createUser, requestedRole, errors);

            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
                usernames.put(usernameKey, existingUserId);
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.members.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.MEMBERS,
                    row.rowNumber(),
                    username,
                    HistoricalFingerprint.of(cooperativeId, "MEMBER", usernameKey),
                    ok,
                    errors,
                    new ValidatedWorkbook.MemberDraft(
                            username == null ? null : username.trim(),
                            firstName,
                            lastName,
                            email,
                            trim(phone),
                            nationalId,
                            membershipDate,
                            shareCount == null ? 1 : shareCount,
                            status,
                            role,
                            existingUserId,
                            createUser)));
        }
        putSummary(out, HistoricalImportSheet.MEMBERS, rows.size(), valid, invalid);
    }

    private void validateCampaigns(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> campaignCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.SPECIAL_CAMPAIGNS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String code = required(row, "Campaign Code", errors);
            String name = required(row, "Name", errors);
            BigDecimal suggested = optionalAmount(row, "Suggested Amount", errors);
            BigDecimal target = optionalAmount(row, "Target Amount", errors);
            LocalDate start = optionalDate(row, "Start Date", errors);
            LocalDate end = optionalDate(row, "End Date", errors);
            SpecialCampaignStatus status = parseEnum(
                    row, "Status", SpecialCampaignStatus.class, SpecialCampaignStatus.CLOSED, errors);
            String codeKey = HistoricalFingerprint.normalize(code);
            if (StringUtils.hasText(code) && !seen.add(codeKey)) {
                errors.add(err(row, "Campaign Code", "DUPLICATE_CODE", "Campaign Code is duplicated in this workbook"));
            }
            UUID existing = resolvePreviousCode(cooperativeId, HistoricalImportSheet.SPECIAL_CAMPAIGNS, code);
            if (existing != null) {
                errors.add(err(row, "Campaign Code", "EXISTING_RECORD", "This campaign code was already imported"));
            } else {
                existing = aliasParent(
                        operationalLookup.matchCampaign(cooperativeId, name, start),
                        row,
                        "Campaign Code",
                        errors);
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
                campaignCodes.put(codeKey, existing);
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.campaigns.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.SPECIAL_CAMPAIGNS,
                    row.rowNumber(),
                    code,
                    HistoricalFingerprint.of(cooperativeId, "SPECIAL_CAMPAIGN", codeKey),
                    ok,
                    errors,
                    new ValidatedWorkbook.CampaignDraft(
                            code, name, trim(row.get("Purpose")), suggested, target, start, end, status, existing)));
        }
        putSummary(out, HistoricalImportSheet.SPECIAL_CAMPAIGNS, rows.size(), valid, invalid);
    }

    private void validateContributions(
            UUID cooperativeId, ParsedWorkbook parsed, ValidatedWorkbook out, Map<String, UUID> usernames) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.CONTRIBUTIONS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String username = required(row, "Username", errors);
            Integer year = requiredInt(row, "Year", errors);
            Integer month = requiredInt(row, "Month", errors);
            if (year != null && (year < 2000 || year > 2100)) {
                errors.add(err(row, "Year", "INVALID_YEAR", "Year must be between 2000 and 2100"));
            }
            if (month != null && (month < 1 || month > 12)) {
                errors.add(err(row, "Month", "INVALID_MONTH", "Month must be between 1 and 12"));
            }
            BigDecimal expected = requiredAmount(row, "Expected Amount", errors);
            BigDecimal paid = requiredAmount(row, "Paid Amount", errors);
            boolean paidMoney = paid != null && paid.compareTo(BigDecimal.ZERO) > 0;
            LocalDate paymentDate = paidMoney
                    ? requiredDate(
                            row,
                            "Payment Date",
                            errors,
                            "Payment Date is required for a paid historical contribution because contribution reports use Payment Date for historical date filtering.")
                    : optionalDate(row, "Payment Date", errors);
            if (paymentDate != null && year != null && month != null && month >= 1 && month <= 12) {
                LocalDate periodStart = LocalDate.of(year, month, 1);
                if (paymentDate.isBefore(periodStart)) {
                    errors.add(err(
                            row,
                            "Payment Date",
                            "PERIOD_MISMATCH",
                            "Payment Date cannot be before the contribution Year/Month. Late payment in a later month is allowed."));
                }
            }
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            String periodKey = HistoricalFingerprint.normalize(username) + "|" + year + "|" + month;
            if (year != null && month != null && !seen.add(periodKey)) {
                errors.add(err(row, "Month", "DUPLICATE_PERIOD", "Contribution period is duplicated in this workbook"));
            }
            if (memberId != null && year != null && month != null
                    && contributionRepository.existsByCooperativeIdAndMemberUserIdAndYearAndMonth(
                            cooperativeId, memberId, year, month)) {
                errors.add(err(
                        row,
                        "Month",
                        "EXISTING_CONTRIBUTION",
                        "A contribution already exists for this member and period and will not be overwritten"));
            }
            ContributionStatus status = ContributionService.deriveStatus(
                    expected == null ? BigDecimal.ZERO : expected, paid == null ? BigDecimal.ZERO : paid);
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.contributions.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.CONTRIBUTIONS,
                    row.rowNumber(),
                    username + "-" + year + "-" + month,
                    HistoricalFingerprint.of(
                            cooperativeId,
                            "CONTRIBUTION",
                            HistoricalFingerprint.normalize(username),
                            String.valueOf(year),
                            String.valueOf(month)),
                    ok,
                    errors,
                    new ValidatedWorkbook.ContributionDraft(
                            username,
                            memberId,
                            year == null ? 0 : year,
                            month == null ? 0 : month,
                            expected,
                            paid,
                            paymentDate,
                            trim(row.get("Reference")),
                            trim(row.get("Notes")),
                            status)));
        }
        putSummary(out, HistoricalImportSheet.CONTRIBUTIONS, rows.size(), valid, invalid);
    }

    private void validateSpecialContributions(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> usernames,
            Map<String, UUID> campaignCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.SPECIAL_CONTRIBUTIONS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String username = required(row, "Username", errors);
            String campaignCode = required(row, "Campaign Code", errors);
            BigDecimal amount = requiredPositive(row, "Amount", errors);
            LocalDate date = requiredDate(
                    row,
                    "Contribution Date",
                    errors,
                    "Contribution Date is required because special contribution reports use Contribution Date for historical date filtering.");
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            UUID campaignId = resolveCode(
                    row,
                    "Campaign Code",
                    campaignCode,
                    campaignCodes,
                    HistoricalImportSheet.SPECIAL_CAMPAIGNS,
                    cooperativeId,
                    errors);
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    "SPECIAL_CONTRIBUTION",
                    HistoricalFingerprint.normalize(username),
                    HistoricalFingerprint.normalize(campaignCode),
                    date == null ? "" : date.toString(),
                    amount == null ? "" : amount.toPlainString());
            if (!seen.add(fp)) {
                errors.add(err(row, "Amount", "DUPLICATE_ROW", "Special contribution is duplicated in this workbook"));
            }
            if (alreadyImported(fp)
                    || operationalLookup.hasSpecialContribution(
                            cooperativeId, memberId, campaignId, date, amount)) {
                errors.add(err(
                        row,
                        "Amount",
                        "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                "special contribution " + username + " / " + campaignCode + " / " + date)));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.specialContributions.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.SPECIAL_CONTRIBUTIONS,
                    row.rowNumber(),
                    username + "/" + campaignCode,
                    fp,
                    ok,
                    errors,
                    new ValidatedWorkbook.SpecialDraft(
                            username,
                            memberId,
                            campaignCode,
                            amount,
                            date,
                            trim(row.get("Reference")),
                            trim(row.get("Notes")))));
        }
        putSummary(out, HistoricalImportSheet.SPECIAL_CONTRIBUTIONS, rows.size(), valid, invalid);
    }

    private void validateSocialContributions(
            UUID cooperativeId, ParsedWorkbook parsed, ValidatedWorkbook out, Map<String, UUID> usernames) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.SOCIAL_CONTRIBUTIONS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String username = required(row, "Username", errors);
            BigDecimal amount = requiredPositive(row, "Amount", errors);
            LocalDate date = requiredDate(
                    row,
                    "Contribution Date",
                    errors,
                    "Contribution Date is required because social fund reports use Contribution Date for historical date filtering.");
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    "SOCIAL_CONTRIBUTION",
                    HistoricalFingerprint.normalize(username),
                    date == null ? "" : date.toString(),
                    amount == null ? "" : amount.toPlainString(),
                    HistoricalFingerprint.normalize(row.get("Reference")));
            if (!seen.add(fp)) {
                errors.add(err(row, "Amount", "DUPLICATE_ROW", "Social contribution is duplicated in this workbook"));
            }
            if (alreadyImported(fp)
                    || operationalLookup.hasSocialContribution(cooperativeId, memberId, date, amount)) {
                errors.add(err(
                        row,
                        "Amount",
                        "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                "social contribution " + username + " / " + date + " / " + amount)));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.socialContributions.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.SOCIAL_CONTRIBUTIONS,
                    row.rowNumber(),
                    username,
                    fp,
                    ok,
                    errors,
                    new ValidatedWorkbook.SocialContributionDraft(
                            username, memberId, amount, date, trim(row.get("Reference")), trim(row.get("Notes")))));
        }
        putSummary(out, HistoricalImportSheet.SOCIAL_CONTRIBUTIONS, rows.size(), valid, invalid);
    }

    private void validateSocialDisbursements(
            UUID cooperativeId, ParsedWorkbook parsed, ValidatedWorkbook out, Map<String, UUID> usernames) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.SOCIAL_DISBURSEMENTS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String username = required(row, "Username", errors);
            BigDecimal amount = requiredPositive(row, "Amount", errors);
            LocalDate date = requiredDate(
                    row,
                    "Disbursement Date",
                    errors,
                    "Disbursement Date is required because social fund reports use Disbursement Date for historical date filtering.");
            String reason = required(row, "Reason", errors);
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    "SOCIAL_DISBURSEMENT",
                    HistoricalFingerprint.normalize(username),
                    date == null ? "" : date.toString(),
                    amount == null ? "" : amount.toPlainString(),
                    HistoricalFingerprint.normalize(reason));
            if (!seen.add(fp)) {
                errors.add(err(row, "Amount", "DUPLICATE_ROW", "Social disbursement is duplicated in this workbook"));
            }
            if (alreadyImported(fp)
                    || operationalLookup.hasSocialDisbursement(cooperativeId, memberId, date, amount)) {
                errors.add(err(
                        row,
                        "Amount",
                        "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                "social disbursement " + username + " / " + date + " / " + amount)));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.socialDisbursements.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.SOCIAL_DISBURSEMENTS,
                    row.rowNumber(),
                    username,
                    fp,
                    ok,
                    errors,
                    new ValidatedWorkbook.SocialDisbursementDraft(
                            username, memberId, amount, date, reason, trim(row.get("Notes")))));
        }
        putSummary(out, HistoricalImportSheet.SOCIAL_DISBURSEMENTS, rows.size(), valid, invalid);
    }

    private void validateLoans(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> usernames,
            Map<String, UUID> loanCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.LOANS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String code = required(row, "Loan Code", errors);
            String username = required(row, "Username", errors);
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            BigDecimal principal = firstAmount(row, errors, "Principal Amount", "Approved Amount", "Requested Amount");
            BigDecimal approved = optionalAmount(row, "Approved Amount", errors);
            BigDecimal requested = optionalAmount(row, "Requested Amount", errors);
            if (approved == null) {
                approved = principal;
            }
            if (requested == null) {
                requested = approved;
            }
            BigDecimal rate = optionalAmount(row, "Interest Rate Percent", errors);
            if (rate == null) {
                rate = BigDecimal.ZERO;
            }
            InterestType interestType =
                    parseEnum(row, "Interest Type", InterestType.class, InterestType.FLAT, errors);
            BigDecimal interest = optionalAmount(row, "Interest Amount", errors);
            if (interest == null && principal != null) {
                interest = LoanInterestCalculator.computeInterest(principal, rate, interestType);
            }
            Integer term = requiredInt(row, "Term Months", errors);
            if (term != null && term < 1) {
                errors.add(err(row, "Term Months", "INVALID_TERM", "Term months must be at least 1"));
            }
            LocalDate requestDate = requiredDate(
                    row,
                    "Request Date",
                    errors,
                    "Request Date is required because the Loans report uses Request Date to determine the reporting period.");
            LocalDate approvalDate = requiredDate(
                    row,
                    "Approval Date",
                    errors,
                    "Approval Date is required for a historical disbursed loan.");
            LocalDate disbursementDate = requiredDate(
                    row,
                    "Disbursement Date",
                    errors,
                    "Disbursement Date is required because the loan disbursement ledger uses the date money left the Saving Scheme.");
            LocalDate dueDate = requiredDate(
                    row,
                    "Due Date",
                    errors,
                    "Due Date is required so the historical loan lifecycle can be reported.");
            requireChronology(row, errors, "Request Date", requestDate, "Approval Date", approvalDate);
            requireChronology(row, errors, "Approval Date", approvalDate, "Disbursement Date", disbursementDate);
            requireChronology(row, errors, "Disbursement Date", disbursementDate, "Due Date", dueDate);
            LoanStatus status = parseEnum(row, "Status", LoanStatus.class, LoanStatus.CLOSED, errors);
            if (status != null && !HISTORICAL_LOAN_STATUSES.contains(status)) {
                errors.add(err(
                        row,
                        "Status",
                        "INVALID_STATUS",
                        "Historical loans must be ACTIVE, OVERDUE, CLOSED, or WRITTEN_OFF"));
            }
            BigDecimal outstandingPrincipal = optionalAmount(row, "Outstanding Principal", errors);
            BigDecimal outstandingInterest = optionalAmount(row, "Outstanding Interest", errors);
            String codeKey = HistoricalFingerprint.normalize(code);
            if (StringUtils.hasText(code) && !seen.add(codeKey)) {
                errors.add(err(row, "Loan Code", "DUPLICATE_CODE", "Loan Code is duplicated in this workbook"));
            }
            UUID existing = resolvePreviousCode(cooperativeId, HistoricalImportSheet.LOANS, code);
            if (existing != null) {
                errors.add(err(row, "Loan Code", "EXISTING_RECORD", "This loan code was already imported"));
            } else {
                existing = aliasParent(
                        operationalLookup.matchLoan(cooperativeId, memberId, disbursementDate, principal),
                        row,
                        "Loan Code",
                        errors);
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
                loanCodes.put(codeKey, existing);
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.loans.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.LOANS,
                    row.rowNumber(),
                    code,
                    HistoricalFingerprint.of(cooperativeId, "LOAN", codeKey),
                    ok,
                    errors,
                    new ValidatedWorkbook.LoanDraft(
                            code,
                            username,
                            memberId,
                            requested,
                            approved,
                            principal,
                            rate,
                            interestType,
                            interest,
                            term == null ? 0 : term,
                            outstandingPrincipal,
                            outstandingInterest,
                            requestDate,
                            approvalDate,
                            disbursementDate,
                            dueDate,
                            status,
                            trim(row.get("Purpose")),
                            existing)));
        }
        putSummary(out, HistoricalImportSheet.LOANS, rows.size(), valid, invalid);
    }

    private void validateRepayments(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> usernames,
            Map<String, UUID> loanCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.LOAN_REPAYMENTS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String loanCode = required(row, "Loan Code", errors);
            String username = required(row, "Username", errors);
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            UUID loanId = resolveCode(
                    row, "Loan Code", loanCode, loanCodes, HistoricalImportSheet.LOANS, cooperativeId, errors);
            LocalDate date = requiredDate(
                    row,
                    "Payment Date",
                    errors,
                    "Payment Date is required because loan repayment reports and the financial ledger use Payment Date.");
            BigDecimal total = requiredPositive(row, "Amount Total", errors);
            BigDecimal principal = requiredAmount(row, "Principal Portion", errors);
            BigDecimal interest = requiredAmount(row, "Interest Portion", errors);
            if (total != null && principal != null && interest != null
                    && MoneyUtils.scale(principal.add(interest)).compareTo(MoneyUtils.scale(total)) != 0) {
                errors.add(err(
                        row,
                        "Amount Total",
                        "REPAYMENT_SPLIT",
                        "Amount Total must equal Principal Portion + Interest Portion"));
            }
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    "LOAN_REPAYMENT",
                    HistoricalFingerprint.normalize(loanCode),
                    date == null ? "" : date.toString(),
                    total == null ? "" : total.toPlainString());
            if (!seen.add(fp)) {
                errors.add(err(row, "Payment Date", "DUPLICATE_ROW", "Repayment is duplicated in this workbook"));
            }
            if (alreadyImported(fp) || operationalLookup.hasRepayment(cooperativeId, loanId, date, total)) {
                errors.add(err(
                        row,
                        "Payment Date",
                        "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                "repayment " + loanCode + " / " + date + " / " + total)));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.repayments.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.LOAN_REPAYMENTS,
                    row.rowNumber(),
                    loanCode,
                    fp,
                    ok,
                    errors,
                    new ValidatedWorkbook.RepaymentDraft(
                            loanCode,
                            username,
                            memberId,
                            total,
                            principal,
                            interest,
                            date,
                            trim(row.get("Reference")),
                            trim(row.get("Notes")))));
        }
        putSummary(out, HistoricalImportSheet.LOAN_REPAYMENTS, rows.size(), valid, invalid);
    }

    private void validateFines(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> usernames,
            Map<String, UUID> fineCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.FINES);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String code = required(row, "Fine Code", errors);
            String username = required(row, "Username", errors);
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            FineType type = parseEnum(row, "Fine Type", FineType.class, FineType.MANUAL, errors);
            if (type == FineType.AUTOMATIC) {
                errors.add(err(
                        row,
                        "Fine Type",
                        "AUTOMATIC_NOT_ALLOWED",
                        "Historical fines must be MANUAL so current automatic-fine logic is not triggered"));
            }
            BigDecimal total = requiredPositive(row, "Total Amount", errors);
            BigDecimal paid = requiredAmount(row, "Paid Amount", errors);
            LocalDate issued = requiredDate(
                    row,
                    "Issued Date",
                    errors,
                    "Issued Date is required because the Fines report uses Issued Date to determine the reporting period.");
            LocalDate due = optionalDate(row, "Due Date", errors);
            if (issued != null && due != null && due.isBefore(issued)) {
                errors.add(err(row, "Due Date", "INVALID_SEQUENCE", "Due Date cannot be before Issued Date"));
            }
            FineStatus status = parseEnum(row, "Status", FineStatus.class, null, errors);
            if (total != null && paid != null && paid.compareTo(total) > 0) {
                errors.add(err(row, "Paid Amount", "PAID_EXCEEDS_TOTAL", "Paid amount cannot exceed total amount"));
            }
            FineStatus derived = total != null && paid != null ? deriveFineStatus(total, paid) : null;
            if (status == null && derived != null) {
                status = derived;
            }
            if (status != null
                    && derived != null
                    && status != FineStatus.WAIVED
                    && status != FineStatus.CANCELLED
                    && status != derived) {
                errors.add(err(
                        row,
                        "Status",
                        "STATUS_AMOUNT_MISMATCH",
                        "Fine Status must match Paid Amount versus Total Amount (expected " + derived + ")"));
            }
            String codeKey = HistoricalFingerprint.normalize(code);
            if (StringUtils.hasText(code) && !seen.add(codeKey)) {
                errors.add(err(row, "Fine Code", "DUPLICATE_CODE", "Fine Code is duplicated in this workbook"));
            }
            UUID existing = resolvePreviousCode(cooperativeId, HistoricalImportSheet.FINES, code);
            if (existing != null) {
                errors.add(err(row, "Fine Code", "EXISTING_RECORD", "This fine code was already imported"));
            } else {
                existing = aliasParent(
                        operationalLookup.matchFine(cooperativeId, memberId, issued, total),
                        row,
                        "Fine Code",
                        errors);
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
                fineCodes.put(codeKey, existing);
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.fines.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.FINES,
                    row.rowNumber(),
                    code,
                    HistoricalFingerprint.of(cooperativeId, "FINE", codeKey),
                    ok,
                    errors,
                    new ValidatedWorkbook.FineDraft(
                            code,
                            username,
                            memberId,
                            type,
                            total,
                            paid,
                            issued,
                            due,
                            status,
                            trim(row.get("Reason")),
                            existing)));
        }
        putSummary(out, HistoricalImportSheet.FINES, rows.size(), valid, invalid);
    }

    private void validateFinePayments(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> usernames,
            Map<String, UUID> fineCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.FINE_PAYMENTS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String fineCode = required(row, "Fine Code", errors);
            String username = required(row, "Username", errors);
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            UUID fineId = resolveCode(
                    row, "Fine Code", fineCode, fineCodes, HistoricalImportSheet.FINES, cooperativeId, errors);
            BigDecimal amount = requiredPositive(row, "Amount", errors);
            LocalDate date = requiredDate(
                    row,
                    "Payment Date",
                    errors,
                    "Payment Date is required because fine payment reports and the financial ledger use Payment Date.");
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    "FINE_PAYMENT",
                    HistoricalFingerprint.normalize(fineCode),
                    date == null ? "" : date.toString(),
                    amount == null ? "" : amount.toPlainString());
            if (!seen.add(fp)) {
                errors.add(err(row, "Payment Date", "DUPLICATE_ROW", "Fine payment is duplicated in this workbook"));
            }
            if (alreadyImported(fp) || operationalLookup.hasFinePayment(cooperativeId, fineId, date, amount)) {
                errors.add(err(
                        row,
                        "Payment Date",
                        "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                "fine payment " + fineCode + " / " + date + " / " + amount)));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.finePayments.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.FINE_PAYMENTS,
                    row.rowNumber(),
                    fineCode,
                    fp,
                    ok,
                    errors,
                    new ValidatedWorkbook.FinePaymentDraft(
                            fineCode,
                            username,
                            memberId,
                            amount,
                            date,
                            trim(row.get("Reference")),
                            trim(row.get("Notes")))));
        }
        putSummary(out, HistoricalImportSheet.FINE_PAYMENTS, rows.size(), valid, invalid);
    }

    private void validateInvestments(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> investmentCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.INVESTMENTS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String code = required(row, "Investment Code", errors);
            String name = required(row, "Name", errors);
            BigDecimal amount = requiredPositive(row, "Amount", errors);
            InvestmentStatus status =
                    parseEnum(row, "Status", InvestmentStatus.class, InvestmentStatus.COMPLETED, errors);
            if (status != null && !HISTORICAL_INVESTMENT_STATUSES.contains(status)) {
                errors.add(err(
                        row,
                        "Status",
                        "INVALID_STATUS",
                        "Historical investments cannot be PLANNED"));
            }
            LocalDate investmentDate;
            if (status == InvestmentStatus.CANCELLED) {
                investmentDate = optionalDate(row, "Investment Date", errors);
            } else {
                investmentDate = requiredDate(
                        row,
                        "Investment Date",
                        errors,
                        "Investment Date is required for historical investments. The system cannot use today's date for a historical financial transaction.");
            }
            BigDecimal expectedReturn = optionalAmount(row, "Expected Return Amount", errors);
            LocalDate expectedReturnDate = optionalDate(row, "Expected Return Date", errors);
            BigDecimal remaining = optionalAmount(row, "Remaining Capital", errors);
            BigDecimal capitalReturned = optionalAmount(row, "Total Capital Returned", errors);
            BigDecimal profitReturned = optionalAmount(row, "Total Profit Returned", errors);
            if (capitalReturned == null) {
                capitalReturned = BigDecimal.ZERO;
            }
            if (profitReturned == null) {
                profitReturned = BigDecimal.ZERO;
            }
            if (remaining == null && amount != null) {
                remaining = MoneyUtils.scaleForStorage(amount.subtract(capitalReturned).max(BigDecimal.ZERO));
            }
            String codeKey = HistoricalFingerprint.normalize(code);
            if (StringUtils.hasText(code) && !seen.add(codeKey)) {
                errors.add(err(row, "Investment Code", "DUPLICATE_CODE", "Investment Code is duplicated"));
            }
            UUID existing = resolvePreviousCode(cooperativeId, HistoricalImportSheet.INVESTMENTS, code);
            if (existing != null) {
                errors.add(err(row, "Investment Code", "EXISTING_RECORD", "This investment was already imported"));
            } else {
                existing = aliasParent(
                        operationalLookup.matchInvestment(cooperativeId, name, amount, investmentDate),
                        row,
                        "Investment Code",
                        errors);
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
                investmentCodes.put(codeKey, existing);
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.investments.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.INVESTMENTS,
                    row.rowNumber(),
                    code,
                    HistoricalFingerprint.of(cooperativeId, "INVESTMENT", codeKey),
                    ok,
                    errors,
                    new ValidatedWorkbook.InvestmentDraft(
                            code,
                            name,
                            amount,
                            investmentDate,
                            expectedReturn,
                            expectedReturnDate,
                            remaining,
                            capitalReturned,
                            profitReturned,
                            status,
                            trim(row.get("Description")),
                            existing)));
        }
        putSummary(out, HistoricalImportSheet.INVESTMENTS, rows.size(), valid, invalid);
    }

    private void validateInvestmentReturns(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> investmentCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.INVESTMENT_RETURNS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String code = required(row, "Investment Code", errors);
            UUID investmentId = resolveCode(
                    row,
                    "Investment Code",
                    code,
                    investmentCodes,
                    HistoricalImportSheet.INVESTMENTS,
                    cooperativeId,
                    errors);
            LocalDate date = requiredDate(
                    row,
                    "Return Date",
                    errors,
                    "Return Date is required because investment return ledger entries use Return Date.");
            BigDecimal capital = requiredAmount(row, "Capital Portion", errors);
            BigDecimal profit = requiredAmount(row, "Profit Portion", errors);
            BigDecimal total = requiredPositive(row, "Amount Total", errors);
            if (capital != null && profit != null && total != null
                    && MoneyUtils.scale(capital.add(profit)).compareTo(MoneyUtils.scale(total)) != 0) {
                errors.add(err(
                        row,
                        "Amount Total",
                        "RETURN_SPLIT",
                        "Amount Total must equal Capital Portion + Profit Portion"));
            }
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    "INVESTMENT_RETURN",
                    HistoricalFingerprint.normalize(code),
                    date == null ? "" : date.toString(),
                    total == null ? "" : total.toPlainString());
            if (!seen.add(fp)) {
                errors.add(err(row, "Return Date", "DUPLICATE_ROW", "Investment return is duplicated"));
            }
            if (alreadyImported(fp)
                    || operationalLookup.hasInvestmentReturn(cooperativeId, investmentId, date, total)) {
                errors.add(err(
                        row,
                        "Return Date",
                        "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                "investment return " + code + " / " + date + " / " + total)));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.investmentReturns.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.INVESTMENT_RETURNS,
                    row.rowNumber(),
                    code,
                    fp,
                    ok,
                    errors,
                    new ValidatedWorkbook.InvestmentReturnDraft(
                            code, date, capital, profit, total, trim(row.get("Reference")), trim(row.get("Notes")))));
        }
        putSummary(out, HistoricalImportSheet.INVESTMENT_RETURNS, rows.size(), valid, invalid);
    }

    private void validateIncomeExpenses(
            UUID cooperativeId, ParsedWorkbook parsed, ValidatedWorkbook out, boolean income) {
        HistoricalImportSheet sheet = income ? HistoricalImportSheet.INCOME : HistoricalImportSheet.EXPENSES;
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(sheet);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            LocalDate date = requiredDate(
                    row,
                    "Transaction Date",
                    errors,
                    "Transaction Date is required because income and expense reports and the financial ledger use Transaction Date.");
            BigDecimal amount = requiredPositive(row, "Amount", errors);
            IncomeExpenseCategory defaultCategory =
                    income ? IncomeExpenseCategory.OTHER_INCOME : IncomeExpenseCategory.GENERAL_EXPENSE;
            IncomeExpenseCategory category =
                    parseEnum(row, "Category", IncomeExpenseCategory.class, defaultCategory, errors);
            if (category == IncomeExpenseCategory.ADJUSTMENT) {
                errors.add(err(
                        row,
                        "Category",
                        "ADJUSTMENT_NOT_ALLOWED",
                        "Historical import cannot create ADJUSTMENT ledger entries. Use OTHER_INCOME, GENERAL_EXPENSE, or INTEREST_EXPENSE."));
            }
            LedgerEffect effect = null;
            if (category == IncomeExpenseCategory.OTHER_INCOME) {
                effect = LedgerEffect.CREDIT;
            } else if (category == IncomeExpenseCategory.GENERAL_EXPENSE
                    || category == IncomeExpenseCategory.INTEREST_EXPENSE) {
                effect = LedgerEffect.DEBIT;
            }
            if (income && category != null && category != IncomeExpenseCategory.OTHER_INCOME) {
                errors.add(err(row, "Category", "INVALID_CATEGORY", "Income category must be OTHER_INCOME"));
            }
            if (!income
                    && category != null
                    && category != IncomeExpenseCategory.GENERAL_EXPENSE
                    && category != IncomeExpenseCategory.INTEREST_EXPENSE) {
                errors.add(err(
                        row,
                        "Category",
                        "INVALID_CATEGORY",
                        "Expense category must be GENERAL_EXPENSE or INTEREST_EXPENSE"));
            }
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    income ? "INCOME" : "EXPENSE",
                    date == null ? "" : date.toString(),
                    amount == null ? "" : amount.toPlainString(),
                    category == null ? "" : category.name(),
                    HistoricalFingerprint.normalize(row.get("Reference")));
            if (!seen.add(fp)) {
                errors.add(err(row, "Reference", "DUPLICATE_ROW", "Transaction is duplicated in this workbook"));
            }
            HistoricalOperationalLookup.Match existingTx = operationalLookup.matchIncomeExpense(
                    cooperativeId, date, amount, category, trim(row.get("Reference")));
            if (alreadyImported(fp) || existingTx.found() || existingTx.ambiguous()) {
                errors.add(err(
                        row,
                        "Reference",
                        existingTx.ambiguous() ? "AMBIGUOUS_DUPLICATE" : "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                existingTx.detail() == null
                                        ? (date + " / " + amount + " / " + category)
                                        : existingTx.detail())));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            ValidatedWorkbook.ValidatedRow<ValidatedWorkbook.IncomeExpenseDraft> validated =
                    new ValidatedWorkbook.ValidatedRow<>(
                            sheet,
                            row.rowNumber(),
                            trim(row.get("Reference")),
                            fp,
                            ok,
                            errors,
                            new ValidatedWorkbook.IncomeExpenseDraft(
                                    date,
                                    amount,
                                    category,
                                    effect,
                                    trim(row.get("Reference")),
                                    trim(row.get("Description")),
                                    trim(row.get("Notes")),
                                    income));
            if (income) {
                out.income.add(validated);
            } else {
                out.expenses.add(validated);
            }
        }
        putSummary(out, sheet, rows.size(), valid, invalid);
    }

    private void validatePayouts(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> payoutCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.PAYOUTS);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String code = required(row, "Payout Code", errors);
            LocalDate from = requiredDate(
                    row,
                    "Period From",
                    errors,
                    "Period From is required because payout reports filter by the contribution period range.");
            LocalDate to = requiredDate(
                    row,
                    "Period To",
                    errors,
                    "Period To is required because payout reports filter by the contribution period range.");
            if (from != null && to != null && to.isBefore(from)) {
                errors.add(err(row, "Period To", "INVALID_SEQUENCE", "Period To must be on or after Period From"));
            }
            BigDecimal pool = requiredPositive(row, "Pool Amount", errors);
            BigDecimal eligible = optionalAmount(row, "Eligible Contributions", errors);
            PayoutRunStatus status = parseEnum(row, "Status", PayoutRunStatus.class, PayoutRunStatus.PAID, errors);
            LocalDate payoutDate = optionalDate(row, "Payout Date", errors);
            if (status != PayoutRunStatus.PAID && status != PayoutRunStatus.CONFIRMED) {
                errors.add(err(row, "Status", "INVALID_STATUS", "Historical payouts must be PAID or CONFIRMED"));
            }
            if (status == PayoutRunStatus.PAID && payoutDate == null) {
                errors.add(err(
                        row,
                        "Payout Date",
                        "REQUIRED",
                        "Payout Date is required for PAID payouts because the financial ledger uses the actual date money left the Saving Scheme. Do not substitute Period To."));
            }
            if (payoutDate != null && from != null && payoutDate.isBefore(from)) {
                errors.add(err(
                        row,
                        "Payout Date",
                        "INVALID_SEQUENCE",
                        "Payout Date cannot be before Period From"));
            }
            String codeKey = HistoricalFingerprint.normalize(code);
            if (StringUtils.hasText(code) && !seen.add(codeKey)) {
                errors.add(err(row, "Payout Code", "DUPLICATE_CODE", "Payout Code is duplicated"));
            }
            UUID existing = resolvePreviousCode(cooperativeId, HistoricalImportSheet.PAYOUTS, code);
            if (existing != null) {
                errors.add(err(row, "Payout Code", "EXISTING_RECORD", "This payout was already imported"));
            } else {
                existing = aliasParent(
                        operationalLookup.matchPayout(cooperativeId, from, to, pool),
                        row,
                        "Payout Code",
                        errors);
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
                payoutCodes.put(codeKey, existing);
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.payouts.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.PAYOUTS,
                    row.rowNumber(),
                    code,
                    HistoricalFingerprint.of(cooperativeId, "PAYOUT", codeKey),
                    ok,
                    errors,
                    new ValidatedWorkbook.PayoutDraft(
                            code,
                            trim(row.get("Name")),
                            from,
                            to,
                            payoutDate,
                            pool,
                            eligible == null ? pool : eligible,
                            status,
                            trim(row.get("Notes")),
                            existing)));
        }
        putSummary(out, HistoricalImportSheet.PAYOUTS, rows.size(), valid, invalid);
    }

    private void validatePayoutLines(
            UUID cooperativeId,
            ParsedWorkbook parsed,
            ValidatedWorkbook out,
            Map<String, UUID> usernames,
            Map<String, UUID> payoutCodes) {
        Set<String> seen = new HashSet<>();
        List<ParsedWorkbook.ParsedRow> rows = parsed.rows(HistoricalImportSheet.PAYOUT_LINES);
        int valid = 0;
        int invalid = 0;
        for (ParsedWorkbook.ParsedRow row : rows) {
            List<HistoricalImportError> errors = new ArrayList<>();
            String payoutCode = required(row, "Payout Code", errors);
            String username = required(row, "Username", errors);
            UUID memberId = resolveUsername(row, username, usernames, cooperativeId, errors);
            UUID payoutId = resolveCode(
                    row, "Payout Code", payoutCode, payoutCodes, HistoricalImportSheet.PAYOUTS, cooperativeId, errors);
            BigDecimal eligible = requiredAmount(row, "Eligible Amount", errors);
            BigDecimal percentage = optionalAmount(row, "Percentage", errors);
            BigDecimal payoutAmount = requiredPositive(row, "Payout Amount", errors);
            PayoutLineStatus status = parseEnum(row, "Status", PayoutLineStatus.class, PayoutLineStatus.PAID, errors);
            String fp = HistoricalFingerprint.of(
                    cooperativeId,
                    "PAYOUT_LINE",
                    HistoricalFingerprint.normalize(payoutCode),
                    HistoricalFingerprint.normalize(username),
                    payoutAmount == null ? "" : payoutAmount.toPlainString());
            if (!seen.add(fp)) {
                errors.add(err(row, "Username", "DUPLICATE_ROW", "Payout line is duplicated"));
            }
            if (alreadyImported(fp)
                    || operationalLookup.hasPayoutLine(cooperativeId, payoutId, memberId, payoutAmount)) {
                errors.add(err(
                        row,
                        "Username",
                        "EXISTING_RECORD",
                        HistoricalOperationalLookup.existingMessage(
                                "payout line " + payoutCode + " / " + username + " / " + payoutAmount)));
            }
            boolean ok = errors.isEmpty();
            if (ok) {
                valid++;
            } else {
                invalid++;
                out.errors.addAll(errors);
            }
            out.payoutLines.add(new ValidatedWorkbook.ValidatedRow<>(
                    HistoricalImportSheet.PAYOUT_LINES,
                    row.rowNumber(),
                    payoutCode + "/" + username,
                    fp,
                    ok,
                    errors,
                    new ValidatedWorkbook.PayoutLineDraft(
                            payoutCode,
                            username,
                            memberId,
                            eligible,
                            percentage == null ? BigDecimal.ZERO : percentage,
                            payoutAmount,
                            status)));
        }
        putSummary(out, HistoricalImportSheet.PAYOUT_LINES, rows.size(), valid, invalid);
    }

    private void reconcileLoanTotals(ValidatedWorkbook out) {
        Map<String, List<ValidatedWorkbook.RepaymentDraft>> byLoan = new HashMap<>();
        for (var row : out.repayments) {
            if (row.valid() && row.draft() != null) {
                byLoan.computeIfAbsent(HistoricalFingerprint.normalize(row.draft().loanCode()), k -> new ArrayList<>())
                        .add(row.draft());
            }
        }
        for (var row : out.loans) {
            if (!row.valid() || row.draft() == null || row.draft().existingId() != null) {
                continue;
            }
            ValidatedWorkbook.LoanDraft loan = row.draft();
            List<ValidatedWorkbook.RepaymentDraft> reps =
                    byLoan.getOrDefault(HistoricalFingerprint.normalize(loan.code()), List.of());
            BigDecimal repaidPrincipal = BigDecimal.ZERO;
            BigDecimal repaidInterest = BigDecimal.ZERO;
            for (ValidatedWorkbook.RepaymentDraft r : reps) {
                repaidPrincipal = repaidPrincipal.add(nvl(r.principalPortion()));
                repaidInterest = repaidInterest.add(nvl(r.interestPortion()));
            }
            BigDecimal expectedOutstandingPrincipal =
                    nvl(loan.principalAmount()).subtract(repaidPrincipal).max(BigDecimal.ZERO);
            BigDecimal expectedOutstandingInterest =
                    nvl(loan.interestAmount()).subtract(repaidInterest).max(BigDecimal.ZERO);
            if (loan.outstandingPrincipal() != null
                    && MoneyUtils.scale(loan.outstandingPrincipal())
                                    .compareTo(MoneyUtils.scale(expectedOutstandingPrincipal))
                            != 0) {
                addRowError(
                        out,
                        row,
                        "Outstanding Principal",
                        "LOAN_RECONCILE",
                        "Outstanding principal must equal principal minus imported principal repayments ("
                                + MoneyUtils.scale(expectedOutstandingPrincipal)
                                + ")");
            }
            if (loan.outstandingInterest() != null
                    && MoneyUtils.scale(loan.outstandingInterest())
                                    .compareTo(MoneyUtils.scale(expectedOutstandingInterest))
                            != 0) {
                addRowError(
                        out,
                        row,
                        "Outstanding Interest",
                        "LOAN_RECONCILE",
                        "Outstanding interest must equal interest minus imported interest repayments ("
                                + MoneyUtils.scale(expectedOutstandingInterest)
                                + ")");
            }
            if (loan.status() == LoanStatus.CLOSED
                    && (expectedOutstandingPrincipal.compareTo(BigDecimal.ZERO) != 0
                            || expectedOutstandingInterest.compareTo(BigDecimal.ZERO) != 0)) {
                addRowError(
                        out,
                        row,
                        "Status",
                        "LOAN_RECONCILE",
                        "CLOSED loans must have zero outstanding principal and interest after repayments");
            }
            if ((loan.status() == LoanStatus.ACTIVE || loan.status() == LoanStatus.OVERDUE)
                    && expectedOutstandingPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
                addRowError(
                        out,
                        row,
                        "Status",
                        "LOAN_RECONCILE",
                        "ACTIVE and OVERDUE loans must have outstanding principal after imported repayments");
            }
        }
    }

    private void reconcileFineTotals(ValidatedWorkbook out) {
        Map<String, BigDecimal> paid = new HashMap<>();
        for (var row : out.finePayments) {
            if (row.valid() && row.draft() != null) {
                paid.merge(
                        HistoricalFingerprint.normalize(row.draft().fineCode()),
                        nvl(row.draft().amount()),
                        BigDecimal::add);
            }
        }
        for (var row : out.fines) {
            if (!row.valid() || row.draft() == null || row.draft().existingId() != null) {
                continue;
            }
            BigDecimal paymentSum = paid.getOrDefault(HistoricalFingerprint.normalize(row.draft().code()), BigDecimal.ZERO);
            if (nvl(row.draft().paidAmount()).compareTo(BigDecimal.ZERO) > 0
                    && MoneyUtils.scale(paymentSum).compareTo(MoneyUtils.scale(row.draft().paidAmount())) != 0) {
                addRowError(
                        out,
                        row,
                        "Paid Amount",
                        "FINE_RECONCILE",
                        "Paid Amount must equal the sum of FinePayments for this Fine Code");
            }
            if (nvl(row.draft().paidAmount()).compareTo(BigDecimal.ZERO) > 0
                    && paymentSum.compareTo(BigDecimal.ZERO) == 0) {
                addRowError(
                        out,
                        row,
                        "Paid Amount",
                        "FINE_RECONCILE",
                        "Paid historical fines require FinePayments rows so ledger income can be posted");
            }
        }
    }

    private void reconcilePayoutTotals(ValidatedWorkbook out) {
        Map<String, BigDecimal> lines = new HashMap<>();
        for (var row : out.payoutLines) {
            if (row.valid() && row.draft() != null) {
                lines.merge(
                        HistoricalFingerprint.normalize(row.draft().payoutCode()),
                        nvl(row.draft().payoutAmount()),
                        BigDecimal::add);
            }
        }
        for (var row : out.payouts) {
            if (!row.valid() || row.draft() == null || row.draft().existingId() != null) {
                continue;
            }
            BigDecimal lineSum = lines.getOrDefault(HistoricalFingerprint.normalize(row.draft().code()), BigDecimal.ZERO);
            if (MoneyUtils.scale(lineSum).compareTo(MoneyUtils.scale(nvl(row.draft().poolAmount()))) != 0) {
                addRowError(
                        out,
                        row,
                        "Pool Amount",
                        "PAYOUT_RECONCILE",
                        "Payout line amounts must sum exactly to the payout pool amount");
            }
        }
    }

    private HistoricalReconciliationSummary buildReconciliation(UUID cooperativeId, ValidatedWorkbook out) {
        BigDecimal current = financialCalculationService.calculateAvailableGroupFund(cooperativeId);
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal outstandingLoans = BigDecimal.ZERO;
        BigDecimal socialIn = BigDecimal.ZERO;
        BigDecimal socialOut = BigDecimal.ZERO;
        BigDecimal payouts = BigDecimal.ZERO;
        for (var row : out.contributions) {
            if (row.valid() && row.draft() != null && nvl(row.draft().paidAmount()).compareTo(BigDecimal.ZERO) > 0) {
                credits = credits.add(row.draft().paidAmount());
            }
        }
        for (var row : out.specialContributions) {
            if (row.valid() && row.draft() != null) {
                credits = credits.add(nvl(row.draft().amount()));
            }
        }
        for (var row : out.finePayments) {
            if (row.valid() && row.draft() != null) {
                credits = credits.add(nvl(row.draft().amount()));
            }
        }
        for (var row : out.income) {
            if (row.valid() && row.draft() != null && row.draft().ledgerEffect() == LedgerEffect.CREDIT) {
                credits = credits.add(nvl(row.draft().amount()));
            }
        }
        for (var row : out.investmentReturns) {
            if (row.valid() && row.draft() != null) {
                credits = credits.add(nvl(row.draft().capitalPortion())).add(nvl(row.draft().profitPortion()));
            }
        }
        for (var row : out.expenses) {
            if (row.valid() && row.draft() != null && row.draft().ledgerEffect() == LedgerEffect.DEBIT) {
                debits = debits.add(nvl(row.draft().amount()));
            }
        }
        for (var row : out.investments) {
            if (row.valid()
                    && row.draft() != null
                    && row.draft().existingId() == null
                    && row.draft().status() != InvestmentStatus.CANCELLED) {
                debits = debits.add(nvl(row.draft().amount()));
            }
        }
        for (var row : out.payoutLines) {
            if (row.valid() && row.draft() != null) {
                BigDecimal amount = nvl(row.draft().payoutAmount());
                debits = debits.add(amount);
                payouts = payouts.add(amount);
            }
        }
        for (var row : out.loans) {
            if (row.valid()
                    && row.draft() != null
                    && row.draft().existingId() == null
                    && (row.draft().status() == LoanStatus.ACTIVE || row.draft().status() == LoanStatus.OVERDUE)) {
                outstandingLoans = outstandingLoans.add(
                        row.draft().outstandingPrincipal() == null
                                ? nvl(row.draft().principalAmount())
                                : row.draft().outstandingPrincipal());
            }
        }
        for (var row : out.socialContributions) {
            if (row.valid() && row.draft() != null) {
                socialIn = socialIn.add(nvl(row.draft().amount()));
            }
        }
        for (var row : out.socialDisbursements) {
            if (row.valid() && row.draft() != null) {
                socialOut = socialOut.add(nvl(row.draft().amount()));
            }
        }
        BigDecimal projected = MoneyUtils.subtract(
                MoneyUtils.subtract(MoneyUtils.add(current, MoneyUtils.scale(credits)), MoneyUtils.scale(debits)),
                MoneyUtils.scale(outstandingLoans));
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        if (projected.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Projected available group fund after import is negative (" + projected + ")");
        }
        addParentMatchWarnings(out, warnings);
        if (credits.compareTo(BigDecimal.ZERO) > 0
                && payouts.compareTo(BigDecimal.ZERO) == 0
                && out.investments.stream()
                        .noneMatch(r -> r.valid() && r.draft() != null && r.draft().existingId() == null)
                && out.expenses.stream().noneMatch(ValidatedWorkbook.ValidatedRow::valid)
                && out.loans.stream()
                        .noneMatch(r -> r.valid() && r.draft() != null && r.draft().existingId() == null)) {
            warnings.add(
                    "Workbook adds fund credits without payouts, expenses, investments, or loans. Confirm only if this cash is still held by the scheme.");
        }
        if (socialOut.compareTo(socialIn.add(socialFundBalanceService.calculateBalance(cooperativeId))) > 0) {
            errors.add("Historical social disbursements exceed current social balance plus imported social contributions");
        }
        boolean blocked = !errors.isEmpty() || out.hasErrors();
        return HistoricalReconciliationSummary.builder()
                .currentAvailableFund(MoneyUtils.scale(current))
                .projectedCredits(MoneyUtils.scale(credits))
                .projectedDebits(MoneyUtils.scale(debits))
                .projectedOutstandingLoanPrincipal(MoneyUtils.scale(outstandingLoans))
                .projectedAvailableFund(projected)
                .projectedSocialContributions(MoneyUtils.scale(socialIn))
                .projectedSocialDisbursements(MoneyUtils.scale(socialOut))
                .projectedSocialBalance(MoneyUtils.scale(
                        socialFundBalanceService.calculateBalance(cooperativeId).add(socialIn).subtract(socialOut)))
                .projectedPayouts(MoneyUtils.scale(payouts))
                .blocked(blocked)
                .warnings(warnings)
                .errors(errors)
                .build();
    }

    private void addRowError(
            ValidatedWorkbook out,
            ValidatedWorkbook.ValidatedRow<?> row,
            String field,
            String code,
            String message) {
        HistoricalImportError error = HistoricalImportError.builder()
                .sheet(row.sheet().getSheetName())
                .rowNumber(row.rowNumber())
                .field(field)
                .code(code)
                .message(message)
                .build();
        out.errors.add(error);
        row.errors().add(error);
        replaceValidity(out, row);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void replaceValidity(ValidatedWorkbook out, ValidatedWorkbook.ValidatedRow<?> row) {
        // ValidatedRow is a record; rebuild the matching list entry as invalid.
        List target = switch (row.sheet()) {
            case MEMBERS -> out.members;
            case CONTRIBUTIONS -> out.contributions;
            case SPECIAL_CAMPAIGNS -> out.campaigns;
            case SPECIAL_CONTRIBUTIONS -> out.specialContributions;
            case SOCIAL_CONTRIBUTIONS -> out.socialContributions;
            case SOCIAL_DISBURSEMENTS -> out.socialDisbursements;
            case LOANS -> out.loans;
            case LOAN_REPAYMENTS -> out.repayments;
            case FINES -> out.fines;
            case FINE_PAYMENTS -> out.finePayments;
            case INVESTMENTS -> out.investments;
            case INVESTMENT_RETURNS -> out.investmentReturns;
            case INCOME -> out.income;
            case EXPENSES -> out.expenses;
            case PAYOUTS -> out.payouts;
            case PAYOUT_LINES -> out.payoutLines;
        };
        for (int i = 0; i < target.size(); i++) {
            ValidatedWorkbook.ValidatedRow<?> current = (ValidatedWorkbook.ValidatedRow<?>) target.get(i);
            if (current.rowNumber() == row.rowNumber() && current.sheet() == row.sheet()) {
                target.set(
                        i,
                        new ValidatedWorkbook.ValidatedRow<>(
                                row.sheet(),
                                row.rowNumber(),
                                row.sourceKey(),
                                row.fingerprint(),
                                false,
                                row.errors(),
                                row.draft()));
                HistoricalImportSheetSummary summary = out.sheetSummaries.get(row.sheet());
                if (summary != null && current.valid()) {
                    summary.setValidRows(Math.max(0, summary.getValidRows() - 1));
                    summary.setInvalidRows(summary.getInvalidRows() + 1);
                }
                break;
            }
        }
    }

    private String resolveImportedRole(
            ParsedWorkbook.ParsedRow row,
            UserPrincipal actor,
            UUID cooperativeId,
            UUID existingUserId,
            boolean createUser,
            String requestedRole,
            List<HistoricalImportError> errors) {
        String role = requestedRole == null ? "MEMBER" : requestedRole;
        if (existingUserId != null && !createUser) {
            return membershipRepository
                    .findByCooperativeIdAndUserId(cooperativeId, existingUserId)
                    .map(membership -> {
                        String current = membership.getRoleInCooperative() == null
                                ? "MEMBER"
                                : CooperativeOfficerRoles.normalize(membership.getRoleInCooperative());
                        if (!current.equals(role) && CooperativeOfficerRoles.isOfficerRoleCode(role)) {
                            errors.add(err(
                                    row,
                                    "Role",
                                    "ROLE_NOT_ALLOWED",
                                    "Historical import cannot change or elevate an existing member's role. Current role is "
                                            + current
                                            + ". Use the Members screen."));
                        }
                        return current;
                    })
                    .orElseGet(() -> {
                        if (CooperativeOfficerRoles.isOfficerRoleCode(role)) {
                            errors.add(err(
                                    row,
                                    "Role",
                                    "ROLE_NOT_ALLOWED",
                                    "Historical import cannot assign officer roles to an existing user. Import as MEMBER and use the Members screen."));
                            return "MEMBER";
                        }
                        return "MEMBER";
                    });
        }
        if (!"MEMBER".equals(role) && !CooperativeOfficerRoles.canAssign(actor, role)) {
            errors.add(err(
                    row,
                    "Role",
                    "ROLE_NOT_ALLOWED",
                    "You cannot assign role " + role + " during historical import"));
            return "MEMBER";
        }
        return role;
    }

    private UUID resolveUsername(
            ParsedWorkbook.ParsedRow row,
            String username,
            Map<String, UUID> workbookUsers,
            UUID cooperativeId,
            List<HistoricalImportError> errors) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        String key = HistoricalFingerprint.normalize(username);
        if (workbookUsers.containsKey(key)) {
            UUID id = workbookUsers.get(key);
            if (id != null) {
                return id;
            }
            return null;
        }
        Optional<User> existing = userRepository.findByUsernameIgnoreCaseAndDeletedFalse(username.trim());
        if (existing.isEmpty()) {
            errors.add(err(
                    row,
                    "Username",
                    "MEMBER_NOT_FOUND",
                    "Username was not found in the Members sheet or this Saving Scheme"));
            return null;
        }
        if (!membershipRepository.existsByCooperativeIdAndUserId(cooperativeId, existing.get().getId())) {
            errors.add(err(
                    row,
                    "Username",
                    "MEMBER_NOT_FOUND",
                    "Username is not a member of this Saving Scheme and is not listed on the Members sheet"));
            return null;
        }
        return existing.get().getId();
    }

    private UUID aliasParent(
            HistoricalOperationalLookup.Match match,
            ParsedWorkbook.ParsedRow row,
            String field,
            List<HistoricalImportError> errors) {
        if (match == null || (!match.found() && !match.ambiguous())) {
            return null;
        }
        if (match.ambiguous()) {
            errors.add(err(
                    row,
                    field,
                    "AMBIGUOUS_PARENT",
                    "Multiple existing CSAMS records match this row (" + match.detail()
                            + "). Historical import will not guess."));
            return null;
        }
        return match.id();
    }

    private void addParentMatchWarnings(ValidatedWorkbook out, List<String> warnings) {
        for (var row : out.campaigns) {
            if (row.valid() && row.draft() != null && row.draft().existingId() != null) {
                warnings.add("Campaign Code " + row.draft().code()
                        + " matched an existing CSAMS campaign and will not be reinserted.");
            }
        }
        for (var row : out.loans) {
            if (row.valid() && row.draft() != null && row.draft().existingId() != null) {
                warnings.add("Loan Code " + row.draft().code()
                        + " matched an existing CSAMS loan and will not be reinserted.");
            }
        }
        for (var row : out.fines) {
            if (row.valid() && row.draft() != null && row.draft().existingId() != null) {
                warnings.add("Fine Code " + row.draft().code()
                        + " matched an existing CSAMS fine and will not be reinserted.");
            }
        }
        for (var row : out.investments) {
            if (row.valid() && row.draft() != null && row.draft().existingId() != null) {
                warnings.add("Investment Code " + row.draft().code()
                        + " matched an existing CSAMS investment and will not be reinserted.");
            }
        }
        for (var row : out.payouts) {
            if (row.valid() && row.draft() != null && row.draft().existingId() != null) {
                warnings.add("Payout Code " + row.draft().code()
                        + " matched an existing CSAMS payout and will not be reinserted.");
            }
        }
    }

    private UUID resolveCode(
            ParsedWorkbook.ParsedRow row,
            String field,
            String code,
            Map<String, UUID> workbookCodes,
            HistoricalImportSheet parentSheet,
            UUID cooperativeId,
            List<HistoricalImportError> errors) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String key = HistoricalFingerprint.normalize(code);
        if (workbookCodes.containsKey(key)) {
            return workbookCodes.get(key);
        }
        UUID previous = resolvePreviousCode(cooperativeId, parentSheet, code);
        if (previous != null) {
            return previous;
        }
        errors.add(err(
                row,
                field,
                "PARENT_NOT_FOUND",
                field + ": " + code + " was not found in " + parentSheet.getSheetName()
                        + " sheet or existing CSAMS records. Add a parent row that maps this code to the existing record."));
        return null;
    }

    private UUID resolvePreviousCode(UUID cooperativeId, HistoricalImportSheet sheet, String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        return importRowRepository
                .findConfirmedSource(cooperativeId, sheet.getSheetName(), HistoricalFingerprint.normalize(code))
                .map(r -> r.getResultingEntityId())
                .orElse(null);
    }

    private boolean alreadyImported(String fingerprint) {
        return !importRowRepository.findByFingerprintAndResultingEntityIdIsNotNull(fingerprint).isEmpty();
    }

    private void summarizeEmptySheets(ValidatedWorkbook out, ParsedWorkbook parsed) {
        for (HistoricalImportSheet sheet : HistoricalImportSheet.values()) {
            List<ParsedWorkbook.ParsedRow> rows = parsed.rows(sheet);
            putSummary(out, sheet, rows.size(), 0, rows.size());
        }
    }

    private static HistoricalReconciliationSummary emptyReconciliation() {
        return HistoricalReconciliationSummary.builder()
                .currentAvailableFund(BigDecimal.ZERO)
                .projectedCredits(BigDecimal.ZERO)
                .projectedDebits(BigDecimal.ZERO)
                .projectedOutstandingLoanPrincipal(BigDecimal.ZERO)
                .projectedAvailableFund(BigDecimal.ZERO)
                .projectedSocialContributions(BigDecimal.ZERO)
                .projectedSocialDisbursements(BigDecimal.ZERO)
                .projectedSocialBalance(BigDecimal.ZERO)
                .projectedPayouts(BigDecimal.ZERO)
                .blocked(true)
                .warnings(List.of())
                .errors(List.of("Workbook has structural errors"))
                .build();
    }

    private static void putSummary(
            ValidatedWorkbook out, HistoricalImportSheet sheet, int total, int valid, int invalid) {
        out.sheetSummaries.put(
                sheet,
                HistoricalImportSheetSummary.builder()
                        .sheet(sheet.getSheetName())
                        .totalRows(total)
                        .validRows(valid)
                        .invalidRows(invalid)
                        .build());
    }

    private static FineStatus deriveFineStatus(BigDecimal total, BigDecimal paid) {
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            return FineStatus.UNPAID;
        }
        if (paid.compareTo(total) < 0) {
            return FineStatus.PARTIALLY_PAID;
        }
        return FineStatus.PAID;
    }

    private List<HistoricalYearSummary> buildYearSummaries(ValidatedWorkbook out) {
        Map<Integer, YearCounts> byYear = new TreeMap<>();
        for (var row : out.members) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().membershipDate()).members++;
            }
        }
        for (var row : out.contributions) {
            if (row.valid() && row.draft() != null) {
                int year = row.draft().year() > 0 ? row.draft().year() : yearOrZero(row.draft().paymentDate());
                if (year > 0) {
                    counts(byYear, year).contributions++;
                }
            }
        }
        for (var row : out.specialContributions) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().contributionDate()).specialContributions++;
            }
        }
        for (var row : out.socialContributions) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().contributionDate()).socialContributions++;
            }
        }
        for (var row : out.socialDisbursements) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().disbursementDate()).socialDisbursements++;
            }
        }
        for (var row : out.loans) {
            if (row.valid() && row.draft() != null && row.draft().existingId() == null) {
                LocalDate date = row.draft().requestDate() != null
                        ? row.draft().requestDate()
                        : row.draft().disbursementDate();
                yearOf(byYear, date).loans++;
            }
        }
        for (var row : out.repayments) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().paymentDate()).repayments++;
            }
        }
        for (var row : out.fines) {
            if (row.valid() && row.draft() != null && row.draft().existingId() == null) {
                yearOf(byYear, row.draft().issuedDate()).fines++;
            }
        }
        for (var row : out.finePayments) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().paymentDate()).finePayments++;
            }
        }
        for (var row : out.investments) {
            if (row.valid() && row.draft() != null && row.draft().existingId() == null) {
                yearOf(byYear, row.draft().investmentDate()).investments++;
            }
        }
        for (var row : out.investmentReturns) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().returnDate()).investmentReturns++;
            }
        }
        for (var row : out.income) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().transactionDate()).income++;
            }
        }
        for (var row : out.expenses) {
            if (row.valid() && row.draft() != null) {
                yearOf(byYear, row.draft().transactionDate()).expenses++;
            }
        }
        for (var row : out.payouts) {
            if (row.valid() && row.draft() != null && row.draft().existingId() == null) {
                yearOf(byYear, row.draft().periodFrom()).payouts++;
            }
        }
        List<HistoricalYearSummary> summaries = new ArrayList<>();
        for (YearCounts counts : byYear.values()) {
            if (counts.year <= 0) {
                continue;
            }
            summaries.add(HistoricalYearSummary.builder()
                    .year(counts.year)
                    .members(counts.members)
                    .contributions(counts.contributions)
                    .specialContributions(counts.specialContributions)
                    .socialContributions(counts.socialContributions)
                    .socialDisbursements(counts.socialDisbursements)
                    .loans(counts.loans)
                    .repayments(counts.repayments)
                    .fines(counts.fines)
                    .finePayments(counts.finePayments)
                    .investments(counts.investments)
                    .investmentReturns(counts.investmentReturns)
                    .income(counts.income)
                    .expenses(counts.expenses)
                    .payouts(counts.payouts)
                    .build());
        }
        return summaries;
    }

    private static final class YearCounts {
        private final int year;
        private int members;
        private int contributions;
        private int specialContributions;
        private int socialContributions;
        private int socialDisbursements;
        private int loans;
        private int repayments;
        private int fines;
        private int finePayments;
        private int investments;
        private int investmentReturns;
        private int income;
        private int expenses;
        private int payouts;

        private YearCounts(int year) {
            this.year = year;
        }
    }

    private static YearCounts yearOf(Map<Integer, YearCounts> byYear, LocalDate date) {
        return counts(byYear, yearOrZero(date));
    }

    private static YearCounts counts(Map<Integer, YearCounts> byYear, int year) {
        return byYear.computeIfAbsent(year, YearCounts::new);
    }

    private static int yearOrZero(LocalDate date) {
        return date == null ? 0 : date.getYear();
    }

    private static String required(ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors) {
        String value = trim(row.get(field));
        if (!StringUtils.hasText(value)) {
            errors.add(err(row, field, "REQUIRED", field + " is required"));
        }
        return value;
    }

    private static LocalDate requiredDate(
            ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors) {
        return requiredDate(row, field, errors, field + " is required");
    }

    private static LocalDate requiredDate(
            ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors, String missingMessage) {
        String raw = row.get(field);
        if (!StringUtils.hasText(raw)) {
            errors.add(err(row, field, "REQUIRED", missingMessage));
            return null;
        }
        return parsedDate(row, field, raw, errors);
    }

    private static LocalDate optionalDate(
            ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors) {
        String raw = row.get(field);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return parsedDate(row, field, raw, errors);
    }

    private static LocalDate parsedDate(
            ParsedWorkbook.ParsedRow row, String field, String raw, List<HistoricalImportError> errors) {
        LocalDate date = WorkbookCellReader.parseDate(raw);
        if (date == null) {
            errors.add(err(row, field, "INVALID_DATE", field + " is not a valid date"));
            return null;
        }
        if (date.isAfter(LocalDate.now())) {
            errors.add(err(row, field, "FUTURE_DATE", field + " cannot be in the future"));
        }
        return date;
    }

    private static void requireChronology(
            ParsedWorkbook.ParsedRow row,
            List<HistoricalImportError> errors,
            String earlierField,
            LocalDate earlier,
            String laterField,
            LocalDate later) {
        if (earlier != null && later != null && later.isBefore(earlier)) {
            errors.add(err(
                    row,
                    laterField,
                    "INVALID_SEQUENCE",
                    laterField + " must be on or after " + earlierField));
        }
    }

    private static BigDecimal requiredAmount(
            ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors) {
        String raw = row.get(field);
        if (!StringUtils.hasText(raw)) {
            errors.add(err(row, field, "REQUIRED", field + " is required"));
            return null;
        }
        try {
            BigDecimal amount = WorkbookCellReader.parseAmount(raw);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                errors.add(err(row, field, "NEGATIVE_AMOUNT", field + " must be >= 0"));
                return null;
            }
            return amount;
        } catch (Exception ex) {
            errors.add(err(row, field, "INVALID_AMOUNT", field + " is not a valid number"));
            return null;
        }
    }

    private static BigDecimal requiredPositive(
            ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors) {
        BigDecimal amount = requiredAmount(row, field, errors);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(err(row, field, "NOT_POSITIVE", field + " must be greater than 0"));
            return null;
        }
        return amount;
    }

    private static BigDecimal optionalAmount(
            ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors) {
        String raw = row.get(field);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            BigDecimal amount = WorkbookCellReader.parseAmount(raw);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                errors.add(err(row, field, "NEGATIVE_AMOUNT", field + " must be >= 0"));
                return null;
            }
            return amount;
        } catch (Exception ex) {
            errors.add(err(row, field, "INVALID_AMOUNT", field + " is not a valid number"));
            return null;
        }
    }

    private static BigDecimal firstAmount(
            ParsedWorkbook.ParsedRow row, List<HistoricalImportError> errors, String... fields) {
        for (String field : fields) {
            if (StringUtils.hasText(row.get(field))) {
                return requiredAmount(row, field, errors);
            }
        }
        errors.add(err(row, fields[0], "REQUIRED", fields[0] + " is required"));
        return null;
    }

    private static Integer requiredInt(ParsedWorkbook.ParsedRow row, String field, List<HistoricalImportError> errors) {
        String raw = row.get(field);
        if (!StringUtils.hasText(raw)) {
            errors.add(err(row, field, "REQUIRED", field + " is required"));
            return null;
        }
        try {
            return Integer.parseInt(raw.trim().replace(",", "").split("\\.")[0]);
        } catch (NumberFormatException ex) {
            errors.add(err(row, field, "INVALID_NUMBER", field + " is not a valid integer"));
            return null;
        }
    }

    private static Integer optionalInt(
            ParsedWorkbook.ParsedRow row, String field, int defaultValue, List<HistoricalImportError> errors) {
        String raw = row.get(field);
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim().replace(",", "").split("\\.")[0]);
        } catch (NumberFormatException ex) {
            errors.add(err(row, field, "INVALID_NUMBER", field + " is not a valid integer"));
            return defaultValue;
        }
    }

    private static <E extends Enum<E>> E parseEnum(
            ParsedWorkbook.ParsedRow row,
            String field,
            Class<E> type,
            E defaultValue,
            List<HistoricalImportError> errors) {
        String raw = row.get(field);
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            errors.add(err(
                    row,
                    field,
                    "INVALID_ENUM",
                    field + " must be one of: " + String.join(", ", enumNames(type))));
            return defaultValue;
        }
    }

    private static <E extends Enum<E>> List<String> enumNames(Class<E> type) {
        List<String> names = new ArrayList<>();
        for (E value : type.getEnumConstants()) {
            names.add(value.name());
        }
        return names;
    }

    private static HistoricalImportError err(
            ParsedWorkbook.ParsedRow row, String field, String code, String message) {
        return HistoricalImportError.builder()
                .sheet(row.sheet().getSheetName())
                .rowNumber(row.rowNumber())
                .field(field)
                .code(code)
                .message(row.sheet().getSheetName() + " / row " + row.rowNumber() + " / " + field + ": " + message)
                .build();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String defaulted(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
