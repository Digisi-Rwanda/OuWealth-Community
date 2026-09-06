package rw.terimbere.csams.modules.historicalimport.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rw.terimbere.csams.modules.contribution.entity.Contribution;
import rw.terimbere.csams.modules.contribution.entity.ContributionReviewStatus;
import rw.terimbere.csams.modules.contribution.entity.ContributionStatus;
import rw.terimbere.csams.modules.contribution.repository.ContributionRepository;
import rw.terimbere.csams.modules.cooperative.entity.Cooperative;
import rw.terimbere.csams.modules.fine.entity.Fine;
import rw.terimbere.csams.modules.fine.entity.FineCalculationMode;
import rw.terimbere.csams.modules.fine.entity.FinePayment;
import rw.terimbere.csams.modules.fine.entity.FinePaymentMethod;
import rw.terimbere.csams.modules.fine.entity.FinePaymentStatus;
import rw.terimbere.csams.modules.fine.entity.FineType;
import rw.terimbere.csams.modules.fine.repository.FinePaymentRepository;
import rw.terimbere.csams.modules.fine.repository.FineRepository;
import rw.terimbere.csams.modules.historicalimport.dto.HistoricalImportConfirmResponse;
import rw.terimbere.csams.modules.historicalimport.entity.HistoricalImportSheet;
import rw.terimbere.csams.modules.historicalimport.repository.HistoricalImportRowRepository;
import rw.terimbere.csams.modules.incomeexpense.entity.IncomeExpenseApprovalStatus;
import rw.terimbere.csams.modules.incomeexpense.entity.IncomeExpenseCategory;
import rw.terimbere.csams.modules.incomeexpense.entity.IncomeExpenseTransaction;
import rw.terimbere.csams.modules.incomeexpense.repository.IncomeExpenseTransactionRepository;
import rw.terimbere.csams.modules.investment.entity.Investment;
import rw.terimbere.csams.modules.investment.entity.InvestmentReturn;
import rw.terimbere.csams.modules.investment.entity.InvestmentStatus;
import rw.terimbere.csams.modules.investment.repository.InvestmentRepository;
import rw.terimbere.csams.modules.investment.repository.InvestmentReturnRepository;
import rw.terimbere.csams.modules.ledger.service.LedgerService;
import rw.terimbere.csams.modules.loan.entity.Loan;
import rw.terimbere.csams.modules.loan.entity.LoanGuaranteeMode;
import rw.terimbere.csams.modules.loan.entity.LoanStatus;
import rw.terimbere.csams.modules.loan.repository.LoanRepository;
import rw.terimbere.csams.modules.loanrepayment.entity.LoanRepayment;
import rw.terimbere.csams.modules.loanrepayment.repository.LoanRepaymentRepository;
import rw.terimbere.csams.modules.membership.entity.CooperativeMembership;
import rw.terimbere.csams.modules.membership.repository.CooperativeMembershipRepository;
import rw.terimbere.csams.modules.payout.entity.PayoutLine;
import rw.terimbere.csams.modules.payout.entity.PayoutLineStatus;
import rw.terimbere.csams.modules.payout.entity.PayoutRun;
import rw.terimbere.csams.modules.payout.repository.PayoutLineRepository;
import rw.terimbere.csams.modules.payout.repository.PayoutRunRepository;
import rw.terimbere.csams.modules.role.entity.Role;
import rw.terimbere.csams.modules.role.repository.RoleRepository;
import rw.terimbere.csams.modules.socialfund.entity.SocialContribution;
import rw.terimbere.csams.modules.socialfund.entity.SocialContributionStatus;
import rw.terimbere.csams.modules.socialfund.entity.SocialDisbursement;
import rw.terimbere.csams.modules.socialfund.entity.SocialDisbursementStatus;
import rw.terimbere.csams.modules.socialfund.repository.SocialContributionRepository;
import rw.terimbere.csams.modules.socialfund.repository.SocialDisbursementRepository;
import rw.terimbere.csams.modules.specialcontribution.entity.SpecialContribution;
import rw.terimbere.csams.modules.specialcontribution.entity.SpecialContributionCampaign;
import rw.terimbere.csams.modules.specialcontribution.entity.SpecialContributionStatus;
import rw.terimbere.csams.modules.specialcontribution.repository.SpecialContributionCampaignRepository;
import rw.terimbere.csams.modules.specialcontribution.repository.SpecialContributionRepository;
import rw.terimbere.csams.modules.user.entity.AccountStatus;
import rw.terimbere.csams.modules.user.entity.User;
import rw.terimbere.csams.modules.user.repository.UserRepository;
import rw.terimbere.csams.security.CooperativeOfficerRoles;
import rw.terimbere.csams.shared.financial.LedgerTransactionType;
import rw.terimbere.csams.shared.utilities.MoneyUtils;

/**
 * Internal insert-only historical persist path. Package-private so live controllers cannot call it.
 */
@Service
@RequiredArgsConstructor
class HistoricalPersistenceService {

    private static final String ROLE_MEMBER = "MEMBER";
    private static final String PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CooperativeMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContributionRepository contributionRepository;
    private final SpecialContributionCampaignRepository campaignRepository;
    private final SpecialContributionRepository specialContributionRepository;
    private final SocialContributionRepository socialContributionRepository;
    private final SocialDisbursementRepository socialDisbursementRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final FineRepository fineRepository;
    private final FinePaymentRepository finePaymentRepository;
    private final InvestmentRepository investmentRepository;
    private final InvestmentReturnRepository investmentReturnRepository;
    private final IncomeExpenseTransactionRepository incomeExpenseRepository;
    private final PayoutRunRepository payoutRunRepository;
    private final PayoutLineRepository payoutLineRepository;
    private final HistoricalImportRowRepository importRowRepository;
    private final LedgerService ledgerService;
    private final SecureRandom secureRandom = new SecureRandom();

    PersistResult persist(Cooperative cooperative, ValidatedWorkbook workbook, UUID actorId) {
        Map<String, UUID> usernames = new HashMap<>();
        Map<String, UUID> campaigns = new HashMap<>();
        Map<String, UUID> loans = new HashMap<>();
        Map<String, UUID> fines = new HashMap<>();
        Map<String, UUID> investments = new HashMap<>();
        Map<String, UUID> payouts = new HashMap<>();
        Map<String, UUID> resultIds = new HashMap<>();
        int ledger = 0;
        int members = 0;
        for (var row : workbook.members) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID userId = persistMember(cooperative.getId(), row.draft(), actorId);
            usernames.put(HistoricalFingerprint.normalize(row.draft().username()), userId);
            resultIds.put(rowKey(row), userId);
            members++;
        }
        int campaignCount = 0;
        for (var row : workbook.campaigns) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID id = persistCampaign(cooperative.getId(), row.draft(), actorId);
            campaigns.put(HistoricalFingerprint.normalize(row.draft().code()), id);
            resultIds.put(rowKey(row), id);
            if (row.draft().existingId() == null) {
                campaignCount++;
            }
        }
        int contributionCount = 0;
        for (var row : workbook.contributions) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID id = persistContribution(cooperative, row.draft(), memberId, actorId);
            resultIds.put(rowKey(row), id);
            contributionCount++;
            if (nvl(row.draft().paidAmount()).compareTo(BigDecimal.ZERO) > 0) {
                ledger++;
            }
        }
        int specialCount = 0;
        for (var row : workbook.specialContributions) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID campaignId = resolveParent(
                    campaigns,
                    HistoricalImportSheet.SPECIAL_CAMPAIGNS,
                    row.draft().campaignCode(),
                    cooperative.getId());
            UUID id = persistSpecial(cooperative, row.draft(), memberId, campaignId, actorId);
            resultIds.put(rowKey(row), id);
            specialCount++;
            ledger++;
        }
        int socialIn = 0;
        for (var row : workbook.socialContributions) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID id = persistSocialContribution(cooperative, row.draft(), memberId, actorId);
            resultIds.put(rowKey(row), id);
            socialIn++;
            ledger++;
        }
        int loanCount = 0;
        for (var row : workbook.loans) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID id = persistLoan(cooperative, workbook, row.draft(), memberId, actorId);
            loans.put(HistoricalFingerprint.normalize(row.draft().code()), id);
            resultIds.put(rowKey(row), id);
            if (row.draft().existingId() == null) {
                loanCount++;
                ledger++;
            }
        }
        int fineCount = 0;
        for (var row : workbook.fines) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID id = persistFine(cooperative.getId(), row.draft(), memberId, actorId);
            fines.put(HistoricalFingerprint.normalize(row.draft().code()), id);
            resultIds.put(rowKey(row), id);
            if (row.draft().existingId() == null) {
                fineCount++;
            }
        }
        int investmentCount = 0;
        for (var row : workbook.investments) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID id = persistInvestment(cooperative, row.draft(), actorId);
            investments.put(HistoricalFingerprint.normalize(row.draft().code()), id);
            resultIds.put(rowKey(row), id);
            if (row.draft().existingId() == null) {
                investmentCount++;
                if (row.draft().status() != InvestmentStatus.CANCELLED) {
                    ledger++;
                }
            }
        }
        int repaymentCount = 0;
        for (var row : workbook.repayments) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID loanId = resolveParent(
                    loans, HistoricalImportSheet.LOANS, row.draft().loanCode(), cooperative.getId());
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID id = persistRepayment(cooperative, row.draft(), loanId, memberId, actorId);
            resultIds.put(rowKey(row), id);
            repaymentCount++;
            if (nvl(row.draft().principalPortion()).compareTo(BigDecimal.ZERO) > 0) {
                ledger++;
            }
            if (nvl(row.draft().interestPortion()).compareTo(BigDecimal.ZERO) > 0) {
                ledger++;
            }
        }
        int finePaymentCount = 0;
        for (var row : workbook.finePayments) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID fineId = resolveParent(
                    fines, HistoricalImportSheet.FINES, row.draft().fineCode(), cooperative.getId());
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID id = persistFinePayment(cooperative, row.draft(), fineId, memberId, actorId);
            resultIds.put(rowKey(row), id);
            finePaymentCount++;
            ledger++;
        }
        int returnCount = 0;
        for (var row : workbook.investmentReturns) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID investmentId = resolveParent(
                    investments,
                    HistoricalImportSheet.INVESTMENTS,
                    row.draft().investmentCode(),
                    cooperative.getId());
            UUID id = persistInvestmentReturn(cooperative, row.draft(), investmentId, actorId);
            resultIds.put(rowKey(row), id);
            returnCount++;
            if (nvl(row.draft().capitalPortion()).compareTo(BigDecimal.ZERO) > 0) {
                ledger++;
            }
            if (nvl(row.draft().profitPortion()).compareTo(BigDecimal.ZERO) > 0) {
                ledger++;
            }
        }
        int incomeCount = 0;
        for (var row : workbook.income) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID id = persistIncomeExpense(cooperative, row.draft(), actorId);
            resultIds.put(rowKey(row), id);
            incomeCount++;
            ledger++;
        }
        int expenseCount = 0;
        for (var row : workbook.expenses) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID id = persistIncomeExpense(cooperative, row.draft(), actorId);
            resultIds.put(rowKey(row), id);
            expenseCount++;
            ledger++;
        }
        int socialOut = 0;
        for (var row : workbook.socialDisbursements) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            UUID id = persistSocialDisbursement(cooperative, row.draft(), memberId, actorId);
            resultIds.put(rowKey(row), id);
            socialOut++;
            ledger++;
        }
        int payoutCount = 0;
        for (var row : workbook.payouts) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID id = persistPayout(cooperative, row.draft(), actorId);
            payouts.put(HistoricalFingerprint.normalize(row.draft().code()), id);
            resultIds.put(rowKey(row), id);
            if (row.draft().existingId() == null) {
                payoutCount++;
            }
        }
        Map<String, LocalDate> payoutDates = new HashMap<>();
        for (var row : workbook.payouts) {
            if (row.valid() && row.draft() != null && row.draft().payoutDate() != null) {
                payoutDates.put(HistoricalFingerprint.normalize(row.draft().code()), row.draft().payoutDate());
            }
        }
        int payoutLineCount = 0;
        for (var row : workbook.payoutLines) {
            if (!row.valid() || row.draft() == null) {
                continue;
            }
            UUID runId = resolveParent(
                    payouts, HistoricalImportSheet.PAYOUTS, row.draft().payoutCode(), cooperative.getId());
            UUID memberId = resolveMember(row.draft().memberUserId(), row.draft().username(), usernames);
            LocalDate payoutDate = payoutDates.get(HistoricalFingerprint.normalize(row.draft().payoutCode()));
            UUID id = persistPayoutLine(
                    cooperative,
                    row.draft(),
                    runId,
                    memberId,
                    actorId,
                    payoutDate);
            resultIds.put(rowKey(row), id);
            payoutLineCount++;
            ledger++;
        }
        return new PersistResult(
                HistoricalImportConfirmResponse.builder()
                        .membersImported(members)
                        .contributionsImported(contributionCount)
                        .specialCampaignsImported(campaignCount)
                        .specialContributionsImported(specialCount)
                        .socialContributionsImported(socialIn)
                        .socialDisbursementsImported(socialOut)
                        .loansImported(loanCount)
                        .repaymentsImported(repaymentCount)
                        .finesImported(fineCount)
                        .finePaymentsImported(finePaymentCount)
                        .investmentsImported(investmentCount)
                        .investmentReturnsImported(returnCount)
                        .incomeImported(incomeCount)
                        .expensesImported(expenseCount)
                        .payoutsImported(payoutCount)
                        .payoutLinesImported(payoutLineCount)
                        .ledgerEntriesCreated(ledger)
                        .build(),
                resultIds);
    }

    private UUID persistMember(UUID cooperativeId, ValidatedWorkbook.MemberDraft draft, UUID actorId) {
        User user;
        if (!draft.createUser() && draft.existingUserId() != null) {
            user = userRepository.findByIdAndDeletedFalse(draft.existingUserId()).orElseThrow();
            // Existing users keep their current platform roles. Historical import never elevates them.
        } else {
            Set<Role> roles = new HashSet<>();
            roles.add(requireRole(ROLE_MEMBER));
            String platformRole = CooperativeOfficerRoles.platformRole(draft.role());
            if (platformRole != null) {
                roles.add(requireRole(platformRole));
            }
            String unknownPassword = generateUnknownPassword();
            AccountStatus accountStatus = switch (draft.membershipStatus()) {
                case "INACTIVE" -> AccountStatus.INACTIVE;
                case "SUSPENDED" -> AccountStatus.SUSPENDED;
                default -> AccountStatus.ACTIVE;
            };
            user = User.builder()
                    .username(draft.username())
                    .email(draft.email())
                    .passwordHash(passwordEncoder.encode(unknownPassword))
                    .firstName(draft.firstName())
                    .lastName(draft.lastName())
                    .phone(draft.phone())
                    .nationalId(draft.nationalId())
                    .accountStatus(accountStatus)
                    .roles(roles)
                    .build();
            user = userRepository.save(user);
        }
        final User persistedUser = user;
        membershipRepository
                .findByCooperativeIdAndUserId(cooperativeId, persistedUser.getId())
                .orElseGet(() -> membershipRepository.save(CooperativeMembership.builder()
                        .userId(persistedUser.getId())
                        .cooperativeId(cooperativeId)
                        .membershipStatus(draft.membershipStatus())
                        .membershipDate(requireHistoricalDate(draft.membershipDate(), "Membership Date"))
                        .roleInCooperative(draft.role())
                        .shareCount(draft.shareCount()) // opening/historical balance, not a purchase
                        .build()));
        return persistedUser.getId();
    }

    private UUID persistCampaign(UUID cooperativeId, ValidatedWorkbook.CampaignDraft draft, UUID actorId) {
        if (draft.existingId() != null) {
            return draft.existingId();
        }
        SpecialContributionCampaign campaign = SpecialContributionCampaign.builder()
                .cooperativeId(cooperativeId)
                .name(draft.name())
                .purpose(draft.purpose())
                .suggestedAmount(draft.suggestedAmount())
                .targetAmount(draft.targetAmount())
                .startDate(draft.startDate())
                .endDate(draft.endDate())
                .status(draft.status())
                .createdBy(actorId)
                .build();
        return campaignRepository.save(campaign).getId();
    }

    private UUID persistContribution(
            Cooperative cooperative, ValidatedWorkbook.ContributionDraft draft, UUID memberId, UUID actorId) {
        BigDecimal expected = nvl(draft.expectedAmount());
        BigDecimal paid = nvl(draft.paidAmount());
        Contribution contribution = Contribution.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .year(draft.year())
                .month(draft.month())
                .expectedAmount(expected)
                .paidAmount(paid)
                .outstandingAmount(MoneyUtils.scaleForStorage(expected.subtract(paid).max(BigDecimal.ZERO)))
                .paymentDate(draft.paymentDate())
                .status(draft.status())
                .paymentReference(draft.reference())
                .notes(draft.notes())
                .recordedBy(actorId)
                .reviewStatus(ContributionReviewStatus.APPROVED)
                .ledgerRevision(paid.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0)
                .build();
        contribution = contributionRepository.save(contribution);
        if (paid.compareTo(BigDecimal.ZERO) > 0
                && (draft.status() == ContributionStatus.PAID
                        || draft.status() == ContributionStatus.PARTIALLY_PAID)) {
            LocalDate date = requireHistoricalDate(draft.paymentDate(), "Payment Date");
            ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                    .cooperativeId(cooperative.getId())
                    .memberUserId(memberId)
                    .transactionType(LedgerTransactionType.REGULAR_CONTRIBUTION)
                    .creditAmount(paid)
                    .currency(cooperative.getCurrency())
                    .transactionDate(date)
                    .reference(draft.reference())
                    .sourceEntityType(LedgerService.SOURCE_CONTRIBUTION)
                    .sourceEntityId(contribution.getId())
                    .description("Historical regular contribution " + draft.year() + "-"
                            + String.format(Locale.ROOT, "%02d", draft.month()))
                    .recordedBy(actorId)
                    .approvedBy(actorId)
                    .idempotencyKey(LedgerService.contributionKey(
                            contribution.getId(), LedgerTransactionType.REGULAR_CONTRIBUTION, 1))
                    .build());
        }
        return contribution.getId();
    }

    private UUID persistSpecial(
            Cooperative cooperative,
            ValidatedWorkbook.SpecialDraft draft,
            UUID memberId,
            UUID campaignId,
            UUID actorId) {
        SpecialContribution entity = SpecialContribution.builder()
                .campaignId(campaignId)
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .amount(draft.amount())
                .contributionDate(draft.contributionDate())
                .paymentReference(draft.reference())
                .notes(draft.notes())
                .status(SpecialContributionStatus.APPROVED)
                .reviewedBy(actorId)
                .reviewedAt(Instant.now())
                .recordedBy(actorId)
                .build();
        entity = specialContributionRepository.save(entity);
        ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .transactionType(LedgerTransactionType.SPECIAL_CONTRIBUTION)
                .creditAmount(draft.amount())
                .currency(cooperative.getCurrency())
                .transactionDate(draft.contributionDate())
                .reference(draft.reference())
                .sourceEntityType(LedgerService.SOURCE_SPECIAL_CONTRIBUTION)
                .sourceEntityId(entity.getId())
                .description("Historical special contribution")
                .recordedBy(actorId)
                .approvedBy(actorId)
                .idempotencyKey(LedgerService.specialContributionKey(entity.getId()))
                .build());
        return entity.getId();
    }

    private UUID persistSocialContribution(
            Cooperative cooperative, ValidatedWorkbook.SocialContributionDraft draft, UUID memberId, UUID actorId) {
        SocialContribution entity = SocialContribution.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .amount(draft.amount())
                .contributionDate(draft.contributionDate())
                .paymentReference(draft.reference())
                .notes(draft.notes())
                .status(SocialContributionStatus.APPROVED)
                .submittedBy(actorId)
                .reviewedBy(actorId)
                .reviewedAt(Instant.now())
                .build();
        entity = socialContributionRepository.save(entity);
        ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .transactionType(LedgerTransactionType.SOCIAL_CONTRIBUTION)
                .creditAmount(draft.amount())
                .currency(cooperative.getCurrency())
                .transactionDate(draft.contributionDate())
                .reference(draft.reference())
                .sourceEntityType(LedgerService.SOURCE_SOCIAL_CONTRIBUTION)
                .sourceEntityId(entity.getId())
                .description("Historical social contribution")
                .recordedBy(actorId)
                .approvedBy(actorId)
                .idempotencyKey(LedgerService.socialContributionKey(entity.getId()))
                .build());
        return entity.getId();
    }

    private UUID persistSocialDisbursement(
            Cooperative cooperative, ValidatedWorkbook.SocialDisbursementDraft draft, UUID memberId, UUID actorId) {
        SocialDisbursement entity = SocialDisbursement.builder()
                .cooperativeId(cooperative.getId())
                .beneficiaryMemberUserId(memberId)
                .amount(draft.amount())
                .disbursementDate(draft.disbursementDate())
                .reason(draft.reason())
                .notes(draft.notes())
                .status(SocialDisbursementStatus.APPROVED)
                .requestedBy(actorId)
                .reviewedBy(actorId)
                .reviewedAt(Instant.now())
                .build();
        entity = socialDisbursementRepository.save(entity);
        ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .transactionType(LedgerTransactionType.SOCIAL_DISBURSEMENT)
                .debitAmount(draft.amount())
                .currency(cooperative.getCurrency())
                .transactionDate(draft.disbursementDate())
                .reference("SOCIAL-DISB-" + entity.getId())
                .sourceEntityType(LedgerService.SOURCE_SOCIAL_DISBURSEMENT)
                .sourceEntityId(entity.getId())
                .description("Historical social disbursement: " + draft.reason())
                .recordedBy(actorId)
                .approvedBy(actorId)
                .idempotencyKey(LedgerService.socialDisbursementKey(entity.getId()))
                .build());
        return entity.getId();
    }

    private UUID persistLoan(
            Cooperative cooperative,
            ValidatedWorkbook workbook,
            ValidatedWorkbook.LoanDraft draft,
            UUID memberId,
            UUID actorId) {
        if (draft.existingId() != null) {
            return draft.existingId();
        }
        BigDecimal repaidPrincipal = BigDecimal.ZERO;
        BigDecimal repaidInterest = BigDecimal.ZERO;
        for (var row : workbook.repayments) {
            if (row.valid()
                    && row.draft() != null
                    && HistoricalFingerprint.normalize(draft.code())
                            .equals(HistoricalFingerprint.normalize(row.draft().loanCode()))) {
                repaidPrincipal = repaidPrincipal.add(nvl(row.draft().principalPortion()));
                repaidInterest = repaidInterest.add(nvl(row.draft().interestPortion()));
            }
        }
        BigDecimal outstandingPrincipal = draft.outstandingPrincipal() == null
                ? nvl(draft.principalAmount()).subtract(repaidPrincipal).max(BigDecimal.ZERO)
                : draft.outstandingPrincipal();
        BigDecimal outstandingInterest = draft.outstandingInterest() == null
                ? nvl(draft.interestAmount()).subtract(repaidInterest).max(BigDecimal.ZERO)
                : draft.outstandingInterest();
        Loan loan = Loan.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .requestedAmount(nvl(draft.requestedAmount()))
                .approvedAmount(nvl(draft.approvedAmount()))
                .principalAmount(nvl(draft.principalAmount()))
                .interestRatePercent(nvl(draft.interestRatePercent()))
                .interestType(draft.interestType())
                .termMonths(draft.termMonths())
                .interestAmount(nvl(draft.interestAmount()))
                .outstandingPrincipal(MoneyUtils.scaleForStorage(outstandingPrincipal))
                .outstandingInterest(MoneyUtils.scaleForStorage(outstandingInterest))
                .totalRepaidPrincipal(MoneyUtils.scaleForStorage(repaidPrincipal))
                .totalRepaidInterest(MoneyUtils.scaleForStorage(repaidInterest))
                .requestDate(draft.requestDate())
                .approvalDate(draft.approvalDate())
                .disbursementDate(draft.disbursementDate())
                .dueDate(draft.dueDate())
                .status(draft.status())
                .guaranteeMode(LoanGuaranteeMode.SELF)
                .purpose(draft.purpose())
                .requestedBy(memberId)
                .approvedBy(actorId)
                .disbursedBy(actorId)
                .build();
        loan = loanRepository.save(loan);
        ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .transactionType(LedgerTransactionType.LOAN_DISBURSEMENT)
                .debitAmount(draft.principalAmount())
                .currency(cooperative.getCurrency())
                .transactionDate(draft.disbursementDate())
                .reference("LOAN-" + loan.getId())
                .sourceEntityType(LedgerService.SOURCE_LOAN)
                .sourceEntityId(loan.getId())
                .description("Historical loan disbursement")
                .recordedBy(actorId)
                .approvedBy(actorId)
                .idempotencyKey(LedgerService.loanDisbursementKey(loan.getId()))
                .build());
        return loan.getId();
    }

    private UUID persistRepayment(
            Cooperative cooperative,
            ValidatedWorkbook.RepaymentDraft draft,
            UUID loanId,
            UUID memberId,
            UUID actorId) {
        LoanRepayment repayment = LoanRepayment.builder()
                .loanId(loanId)
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .paymentDate(draft.paymentDate())
                .amountTotal(draft.amountTotal())
                .principalPortion(nvl(draft.principalPortion()))
                .interestPortion(nvl(draft.interestPortion()))
                .paymentReference(draft.reference())
                .notes(draft.notes())
                .recordedBy(actorId)
                .build();
        repayment = loanRepaymentRepository.save(repayment);
        if (nvl(draft.principalPortion()).compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                    .cooperativeId(cooperative.getId())
                    .memberUserId(memberId)
                    .transactionType(LedgerTransactionType.LOAN_PRINCIPAL_REPAYMENT)
                    .creditAmount(draft.principalPortion())
                    .currency(cooperative.getCurrency())
                    .transactionDate(draft.paymentDate())
                    .reference(draft.reference())
                    .sourceEntityType(LedgerService.SOURCE_LOAN_REPAYMENT)
                    .sourceEntityId(repayment.getId())
                    .description("Historical loan principal repayment")
                    .recordedBy(actorId)
                    .approvedBy(actorId)
                    .idempotencyKey(LedgerService.loanPrincipalRepaymentKey(repayment.getId()))
                    .build());
        }
        if (nvl(draft.interestPortion()).compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                    .cooperativeId(cooperative.getId())
                    .memberUserId(memberId)
                    .transactionType(LedgerTransactionType.LOAN_INTEREST_PAYMENT)
                    .creditAmount(draft.interestPortion())
                    .currency(cooperative.getCurrency())
                    .transactionDate(draft.paymentDate())
                    .reference(draft.reference())
                    .sourceEntityType(LedgerService.SOURCE_LOAN_REPAYMENT)
                    .sourceEntityId(repayment.getId())
                    .description("Historical loan interest repayment")
                    .recordedBy(actorId)
                    .approvedBy(actorId)
                    .idempotencyKey(LedgerService.loanInterestRepaymentKey(repayment.getId()))
                    .build());
        }
        return repayment.getId();
    }

    private UUID persistFine(UUID cooperativeId, ValidatedWorkbook.FineDraft draft, UUID memberId, UUID actorId) {
        if (draft.existingId() != null) {
            return draft.existingId();
        }
        Fine fine = Fine.builder()
                .cooperativeId(cooperativeId)
                .memberUserId(memberId)
                .fineType(draft.fineType() == null ? FineType.MANUAL : draft.fineType())
                .calculationMode(FineCalculationMode.FIXED)
                .baseAmount(draft.totalAmount())
                .totalAmount(draft.totalAmount())
                .paidAmount(nvl(draft.paidAmount()))
                .outstandingAmount(MoneyUtils.scaleForStorage(
                        nvl(draft.totalAmount()).subtract(nvl(draft.paidAmount())).max(BigDecimal.ZERO)))
                .reason(draft.reason())
                .issuedDate(draft.issuedDate())
                .dueDate(draft.dueDate())
                .status(draft.status())
                .issuedBy(actorId)
                .build();
        return fineRepository.save(fine).getId();
    }

    private UUID persistFinePayment(
            Cooperative cooperative,
            ValidatedWorkbook.FinePaymentDraft draft,
            UUID fineId,
            UUID memberId,
            UUID actorId) {
        FinePayment payment = FinePayment.builder()
                .fineId(fineId)
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .amount(draft.amount())
                .paymentDate(draft.paymentDate())
                .paymentReference(draft.reference())
                .paymentMethod(FinePaymentMethod.CASH)
                .notes(draft.notes())
                .status(FinePaymentStatus.APPROVED)
                .submittedBy(actorId)
                .reviewedBy(actorId)
                .reviewedAt(Instant.now())
                .build();
        payment = finePaymentRepository.save(payment);
        ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .transactionType(LedgerTransactionType.FINE_PAYMENT)
                .creditAmount(draft.amount())
                .currency(cooperative.getCurrency())
                .transactionDate(draft.paymentDate())
                .reference(draft.reference())
                .sourceEntityType(LedgerService.SOURCE_FINE_PAYMENT)
                .sourceEntityId(payment.getId())
                .description("Historical fine payment")
                .recordedBy(actorId)
                .approvedBy(actorId)
                .idempotencyKey(LedgerService.finePaymentKey(payment.getId()))
                .build());
        return payment.getId();
    }

    private UUID persistInvestment(Cooperative cooperative, ValidatedWorkbook.InvestmentDraft draft, UUID actorId) {
        if (draft.existingId() != null) {
            return draft.existingId();
        }
        Instant activated = draft.status() == InvestmentStatus.CANCELLED
                ? null
                : toStartOfDay(requireHistoricalDate(draft.investmentDate(), "Investment Date"));
        Investment investment = Investment.builder()
                .cooperativeId(cooperative.getId())
                .name(draft.name())
                .description(draft.description())
                .amount(draft.amount())
                .expectedReturnAmount(draft.expectedReturnAmount())
                .expectedReturnDate(draft.expectedReturnDate())
                .remainingCapital(nvl(draft.remainingCapital()))
                .totalCapitalReturned(nvl(draft.totalCapitalReturned()))
                .totalProfitReturned(nvl(draft.totalProfitReturned()))
                .status(draft.status())
                .createdBy(actorId)
                .activatedAt(activated)
                .completedAt(draft.status() == InvestmentStatus.COMPLETED ? activated : null)
                .build();
        investment = investmentRepository.save(investment);
        if (draft.status() != InvestmentStatus.CANCELLED) {
            LocalDate date = requireHistoricalDate(draft.investmentDate(), "Investment Date");
            ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                    .cooperativeId(cooperative.getId())
                    .transactionType(LedgerTransactionType.INVESTMENT_OUTFLOW)
                    .debitAmount(draft.amount())
                    .currency(cooperative.getCurrency())
                    .transactionDate(date)
                    .reference(draft.code())
                    .sourceEntityType(LedgerService.SOURCE_INVESTMENT)
                    .sourceEntityId(investment.getId())
                    .description("Historical investment outflow: " + draft.name())
                    .recordedBy(actorId)
                    .approvedBy(actorId)
                    .idempotencyKey(LedgerService.investmentOutflowKey(investment.getId()))
                    .build());
        }
        return investment.getId();
    }

    private UUID persistInvestmentReturn(
            Cooperative cooperative, ValidatedWorkbook.InvestmentReturnDraft draft, UUID investmentId, UUID actorId) {
        InvestmentReturn entity = InvestmentReturn.builder()
                .investmentId(investmentId)
                .cooperativeId(cooperative.getId())
                .returnDate(draft.returnDate())
                .capitalPortion(nvl(draft.capitalPortion()))
                .profitPortion(nvl(draft.profitPortion()))
                .amountTotal(draft.amountTotal())
                .reference(draft.reference())
                .notes(draft.notes())
                .recordedBy(actorId)
                .build();
        entity = investmentReturnRepository.save(entity);
        if (nvl(draft.capitalPortion()).compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                    .cooperativeId(cooperative.getId())
                    .transactionType(LedgerTransactionType.INVESTMENT_CAPITAL_RETURN)
                    .creditAmount(draft.capitalPortion())
                    .currency(cooperative.getCurrency())
                    .transactionDate(draft.returnDate())
                    .reference(draft.reference())
                    .sourceEntityType(LedgerService.SOURCE_INVESTMENT_RETURN)
                    .sourceEntityId(entity.getId())
                    .description("Historical investment capital return")
                    .recordedBy(actorId)
                    .approvedBy(actorId)
                    .idempotencyKey(LedgerService.investmentCapitalReturnKey(entity.getId()))
                    .build());
        }
        if (nvl(draft.profitPortion()).compareTo(BigDecimal.ZERO) > 0) {
            ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                    .cooperativeId(cooperative.getId())
                    .transactionType(LedgerTransactionType.INVESTMENT_PROFIT)
                    .creditAmount(draft.profitPortion())
                    .currency(cooperative.getCurrency())
                    .transactionDate(draft.returnDate())
                    .reference(draft.reference())
                    .sourceEntityType(LedgerService.SOURCE_INVESTMENT_RETURN)
                    .sourceEntityId(entity.getId())
                    .description("Historical investment profit")
                    .recordedBy(actorId)
                    .approvedBy(actorId)
                    .idempotencyKey(LedgerService.investmentProfitKey(entity.getId()))
                    .build());
        }
        return entity.getId();
    }

    private UUID persistIncomeExpense(
            Cooperative cooperative, ValidatedWorkbook.IncomeExpenseDraft draft, UUID actorId) {
        if (draft.category() == IncomeExpenseCategory.ADJUSTMENT) {
            throw new IllegalStateException("Historical import cannot persist ADJUSTMENT transactions");
        }
        IncomeExpenseTransaction tx = IncomeExpenseTransaction.builder()
                .cooperativeId(cooperative.getId())
                .category(draft.category())
                .amount(draft.amount())
                .ledgerEffect(null)
                .transactionDate(draft.transactionDate())
                .reference(draft.reference())
                .description(draft.description())
                .notes(draft.notes())
                .approvalStatus(IncomeExpenseApprovalStatus.APPROVED)
                .recordedBy(actorId)
                .approvedBy(actorId)
                .approvedAt(Instant.now())
                .build();
        tx = incomeExpenseRepository.save(tx);
        LedgerTransactionType type = switch (draft.category()) {
            case OTHER_INCOME -> LedgerTransactionType.OTHER_INCOME;
            case GENERAL_EXPENSE -> LedgerTransactionType.GENERAL_EXPENSE;
            case INTEREST_EXPENSE -> LedgerTransactionType.INTEREST_EXPENSE;
            case ADJUSTMENT -> throw new IllegalStateException("ADJUSTMENT is not imported");
        };
        boolean credit = draft.category() == IncomeExpenseCategory.OTHER_INCOME;
        ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                .cooperativeId(cooperative.getId())
                .transactionType(type)
                .creditAmount(credit ? draft.amount() : BigDecimal.ZERO)
                .debitAmount(credit ? BigDecimal.ZERO : draft.amount())
                .currency(cooperative.getCurrency())
                .transactionDate(draft.transactionDate())
                .reference(draft.reference())
                .sourceEntityType(LedgerService.SOURCE_INCOME_EXPENSE)
                .sourceEntityId(tx.getId())
                .description(draft.description() == null ? "Historical income/expense" : draft.description())
                .recordedBy(actorId)
                .approvedBy(actorId)
                .idempotencyKey(LedgerService.incomeExpenseKey(tx.getId(), type))
                .build());
        return tx.getId();
    }

    private UUID persistPayout(Cooperative cooperative, ValidatedWorkbook.PayoutDraft draft, UUID actorId) {
        if (draft.existingId() != null) {
            return draft.existingId();
        }
        Instant paidAt = draft.status() == rw.terimbere.csams.modules.payout.entity.PayoutRunStatus.PAID
                ? toStartOfDay(requireHistoricalDate(draft.payoutDate(), "Payout Date"))
                : (draft.payoutDate() == null ? null : toStartOfDay(draft.payoutDate()));
        PayoutRun run = PayoutRun.builder()
                .cooperativeId(cooperative.getId())
                .name(draft.name())
                .periodFrom(draft.periodFrom())
                .periodTo(draft.periodTo())
                .includeRegular(true)
                .includeSpecial(true)
                .availableFundSnapshot(draft.poolAmount())
                .payoutPoolAmount(draft.poolAmount())
                .totalEligibleContributions(nvl(draft.eligibleContributions()))
                .currency(cooperative.getCurrency())
                .status(draft.status())
                .confirmedAt(Instant.now())
                .confirmedBy(actorId)
                .paidAt(draft.status() == rw.terimbere.csams.modules.payout.entity.PayoutRunStatus.PAID
                        ? paidAt
                        : null)
                .paidBy(actorId)
                .createdBy(actorId)
                .notes(draft.notes())
                .build();
        return payoutRunRepository.save(run).getId();
    }

    private UUID persistPayoutLine(
            Cooperative cooperative,
            ValidatedWorkbook.PayoutLineDraft draft,
            UUID runId,
            UUID memberId,
            UUID actorId,
            LocalDate transactionDate) {
        PayoutLine line = PayoutLine.builder()
                .payoutRunId(runId)
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .eligibleContributionAmount(nvl(draft.eligibleAmount()))
                .percentage(nvl(draft.percentage()))
                .payoutAmount(draft.payoutAmount())
                .status(draft.status() == null ? PayoutLineStatus.PAID : draft.status())
                .build();
        line = payoutLineRepository.save(line);
        ledgerService.appendApproved(LedgerService.AppendRequest.builder()
                .cooperativeId(cooperative.getId())
                .memberUserId(memberId)
                .transactionType(LedgerTransactionType.MEMBER_PAYOUT)
                .debitAmount(draft.payoutAmount())
                .currency(cooperative.getCurrency())
                .transactionDate(requireHistoricalDate(transactionDate, "Payout Date"))
                .reference("PAYOUT-" + runId)
                .sourceEntityType(LedgerService.SOURCE_MEMBER_PAYOUT)
                .sourceEntityId(line.getId())
                .description("Historical member payout")
                .recordedBy(actorId)
                .approvedBy(actorId)
                .idempotencyKey(LedgerService.memberPayoutKey(line.getId()))
                .build());
        return line.getId();
    }

    private UUID resolveMember(UUID existing, String username, Map<String, UUID> created) {
        if (existing != null) {
            return existing;
        }
        return created.get(HistoricalFingerprint.normalize(username));
    }

    private UUID resolveParent(
            Map<String, UUID> created, HistoricalImportSheet sheet, String code, UUID cooperativeId) {
        UUID fromWorkbook = created.get(HistoricalFingerprint.normalize(code));
        if (fromWorkbook != null) {
            return fromWorkbook;
        }
        return importRowRepository
                .findConfirmedSource(cooperativeId, sheet.getSheetName(), HistoricalFingerprint.normalize(code))
                .map(row -> row.getResultingEntityId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unresolved " + sheet.getSheetName() + " code '" + code + "' during confirm"));
    }

    private Role requireRole(String code) {
        return roleRepository.findByCode(code).orElseThrow(() -> new IllegalStateException("Missing role " + code));
    }

    private String generateUnknownPassword() {
        char[] chars = PASSWORD_CHARS.toCharArray();
        StringBuilder builder = new StringBuilder(48);
        for (int i = 0; i < 48; i++) {
            builder.append(chars[secureRandom.nextInt(chars.length)]);
        }
        return builder.toString();
    }

    private static String rowKey(ValidatedWorkbook.ValidatedRow<?> row) {
        return row.sheet().getSheetName() + ":" + row.rowNumber();
    }

    private static Instant toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static LocalDate requireHistoricalDate(LocalDate date, String field) {
        if (date == null) {
            throw new IllegalStateException(
                    field + " is required for historical persistence and cannot be replaced with today");
        }
        return date;
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    record PersistResult(HistoricalImportConfirmResponse counts, Map<String, UUID> resultingEntityIds) {}
}
