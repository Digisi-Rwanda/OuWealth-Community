package rw.terimbere.csams.modules.historicalimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rw.terimbere.csams.modules.contribution.entity.Contribution;
import rw.terimbere.csams.modules.contribution.repository.ContributionRepository;
import rw.terimbere.csams.modules.cooperative.CooperativeTestFixtures;
import rw.terimbere.csams.modules.historicalimport.entity.HistoricalImportSheet;
import rw.terimbere.csams.modules.historicalimport.entity.HistoricalImportStatus;
import rw.terimbere.csams.modules.historicalimport.repository.HistoricalImportRepository;
import rw.terimbere.csams.modules.ledger.entity.LedgerEntry;
import rw.terimbere.csams.modules.ledger.repository.LedgerEntryRepository;
import rw.terimbere.csams.modules.notification.account.AccountNotificationCopy;
import rw.terimbere.csams.modules.notification.repository.NotificationRepository;
import rw.terimbere.csams.modules.membership.repository.CooperativeMembershipRepository;
import rw.terimbere.csams.modules.payout.repository.PayoutLineRepository;
import rw.terimbere.csams.modules.user.repository.UserRepository;
import rw.terimbere.csams.shared.financial.LedgerTransactionType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoricalImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private HistoricalImportRepository historicalImportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private CooperativeMembershipRepository membershipRepository;

    @Autowired
    private PayoutLineRepository payoutLineRepository;

    private String superAdminToken;
    private UUID cooperativeId;
    private UUID otherCooperativeId;

    @BeforeEach
    void setUp() throws Exception {
        superAdminToken = login("superadmin", "ChangeMe@123!");
        cooperativeId = createCooperative("Hist Coop " + UUID.randomUUID().toString().substring(0, 8));
        otherCooperativeId = createCooperative("Other Hist " + UUID.randomUUID().toString().substring(0, 8));
    }

    @Test
    void templateDownload_containsExpectedSheetsAndHeaders() throws Exception {
        MvcResult result = mockMvc.perform(get(base(cooperativeId) + "/template")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();
        byte[] body = result.getResponse().getContentAsByteArray();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(body))) {
            assertThat(workbook.getSheet("Instructions")).isNotNull();
            StringBuilder instructions = new StringBuilder();
            Sheet instructionSheet = workbook.getSheet("Instructions");
            for (int i = 0; i <= instructionSheet.getLastRowNum(); i++) {
                Row row = instructionSheet.getRow(i);
                if (row != null && row.getCell(0) != null) {
                    instructions.append(row.getCell(0).getStringCellValue()).append('\n');
                }
            }
            assertThat(instructions.toString()).contains("Required Dates for Historical Reporting");
            assertThat(instructions.toString()).contains("cannot use today's date");
            for (HistoricalImportSheet sheet : HistoricalImportSheet.values()) {
                Row header = workbook.getSheet(sheet.getSheetName()).getRow(0);
                assertThat(header.getCell(0).getStringCellValue()).isEqualTo(sheet.getHeaders().get(0));
            }
        }
    }

    @Test
    void authorization_allowsLeadershipAndDeniesOthers() throws Exception {
        String president = registerAndLogin(cooperativeId, "PRESIDENT");
        String vice = registerAndLogin(cooperativeId, "VICE_PRESIDENT");
        String accountant = registerAndLogin(cooperativeId, "ACCOUNTANT");
        String member = registerAndLogin(cooperativeId, "MEMBER");
        MockMultipartFile file = xlsx("ok.xlsx", validMemberAndContribution("hist_ok_" + id8(), "10000"));

        mockMvc.perform(get(base(cooperativeId) + "/template").header("Authorization", "Bearer " + president))
                .andExpect(status().isOk());
        mockMvc.perform(get(base(cooperativeId) + "/template").header("Authorization", "Bearer " + vice))
                .andExpect(status().isOk());
        mockMvc.perform(get(base(cooperativeId) + "/template").header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + accountant))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + member))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart(base(otherCooperativeId) + "/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + president))
                .andExpect(status().isForbidden());
    }

    @Test
    void preview_validWorkbook_andMalformedUnknownHeaderMissingMemberDuplicates() throws Exception {
        String username = "hist_valid_" + id8();
        MvcResult valid = mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("valid.xlsx", validMemberAndContribution(username, "10000")))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.confirmAllowed").value(true))
                .andReturn();
        assertThat(contributionRepository
                        .search(cooperativeId, null, 2022, 3, null, null, null, Pageable.unpaged())
                        .getContent())
                .isEmpty();

        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(new MockMultipartFile("file", "bad.txt", "text/plain", "not-excel".getBytes()))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("unknown.xlsx", Map.of("FinancialLedger", List.of(List.of("a")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"))
                .andExpect(jsonPath("$.data.errors[0].code").value("UNKNOWN_SHEET"));

        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("bad-header.xlsx", workbookWithCustomHeader(
                                "Contributions", List.of("Wrong", "Headers"))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.errors[0].code").value("BAD_HEADER"));

        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "missing-member.xlsx",
                                Map.of(
                                        "Contributions",
                                        List.of(contributionRow("ghost_user", "2022", "3", "10000", "10000", "2022-03-05")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        String dup = "hist_dup_" + id8();
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "dup-user.xlsx",
                                Map.of(
                                        "Members",
                                        List.of(memberRow(dup, "ACTIVE"), memberRow(dup, "ACTIVE")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.errors[0].code").value("DUPLICATE_USERNAME"));

        UUID importId = UUID.fromString(objectMapper
                .readTree(valid.getResponse().getContentAsString())
                .path("data")
                .path("importId")
                .asText());
        assertThat(historicalImportRepository.findById(importId)).isPresent();
    }

    @Test
    void preview_rejectsUnresolvedCodesInconsistentLoansInvalidMoneyDates_andAcceptsInactiveMember()
            throws Exception {
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "loan-missing.xlsx",
                                Map.of(
                                        "Members",
                                        List.of(memberRow("hist_loan_" + id8(), "ACTIVE")),
                                        "LoanRepayments",
                                        List.of(List.of(
                                                "L-MISSING",
                                                "hist_loan_" + id8(),
                                                "2022-08-10",
                                                "1000",
                                                "1000",
                                                "0",
                                                "R1",
                                                "")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "fine-missing.xlsx",
                                Map.of(
                                        "Members",
                                        List.of(memberRow("hist_fine_" + id8(), "ACTIVE")),
                                        "FinePayments",
                                        List.of(List.of("F-MISSING", "hist_fine_" + id8(), "1000", "2022-07-10", "R", "")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        String loanUser = "hist_loanrec_" + id8();
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "loan-bad.xlsx",
                                Map.of(
                                        "Members",
                                        List.of(memberRow(loanUser, "ACTIVE")),
                                        "Loans",
                                        List.of(loanRow(loanUser, "100000", "0", "CLOSED")),
                                        "LoanRepayments",
                                        List.of(List.of(
                                                "L-2022-001",
                                                loanUser,
                                                "2022-08-10",
                                                "50000",
                                                "40000",
                                                "10000",
                                                "R1",
                                                "")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        String moneyUser = "hist_money_" + id8();
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "bad-money.xlsx",
                                Map.of(
                                        "Members",
                                        List.of(memberRow(moneyUser, "ACTIVE")),
                                        "Contributions",
                                        List.of(contributionRow(moneyUser, "2022", "3", "abc", "abc", "2022-03-05")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        String dateUser = "hist_date_" + id8();
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "bad-date.xlsx",
                                Map.of(
                                        "Members",
                                        List.of(memberRow(dateUser, "ACTIVE")),
                                        "Contributions",
                                        List.of(contributionRow(dateUser, "2022", "3", "10000", "10000", "not-a-date")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        String inactive = "hist_inactive_" + id8();
        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("inactive.xlsx", validMemberAndContribution(inactive, "10000", "INACTIVE")))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void confirm_commitsEntitiesAndLedger_keepsHistoricalDates_andRejectsReplay() throws Exception {
        String username = "hist_commit_" + id8();
        byte[] bytes = workbookBytes(validMemberAndContribution(username, "15000"));
        MvcResult preview = mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("commit.xlsx", bytes))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andReturn();
        UUID importId = UUID.fromString(objectMapper
                .readTree(preview.getResponse().getContentAsString())
                .path("data")
                .path("importId")
                .asText());

        mockMvc.perform(post(base(cooperativeId) + "/" + importId + "/confirm")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.membersImported").value(1))
                .andExpect(jsonPath("$.data.contributionsImported").value(1))
                .andExpect(jsonPath("$.data.ledgerEntriesCreated").value(1));

        var user = userRepository.findByUsernameIgnoreCaseAndDeletedFalse(username).orElseThrow();
        assertThat(notificationRepository.findAll().stream()
                        .filter(n -> user.getId().equals(n.getUserId())
                                && AccountNotificationCopy.WELCOME_TITLE.equals(n.getTitle()))
                        .count())
                .isZero();
        Contribution contribution = contributionRepository
                .findByCooperativeIdAndMemberUserIdAndYearAndMonth(cooperativeId, user.getId(), 2022, 3)
                .orElseThrow();
        assertThat(contribution.getPaymentDate()).isEqualTo(LocalDate.of(2022, 3, 5));
        assertThat(contribution.getPaidAmount()).isEqualByComparingTo("15000");
        assertThat(contribution.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate())
                .isEqualTo(LocalDate.now());

        List<LedgerEntry> ledger = ledgerEntryRepository
                .findFiltered(
                        cooperativeId,
                        LedgerTransactionType.REGULAR_CONTRIBUTION,
                        LocalDate.of(2022, 1, 1),
                        LocalDate.of(2022, 12, 31),
                        user.getId(),
                        null,
                        Pageable.unpaged())
                .getContent();
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).getTransactionDate()).isEqualTo(LocalDate.of(2022, 3, 5));
        assertThat(ledger.get(0).getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate())
                .isEqualTo(LocalDate.now());

        mockMvc.perform(post(base(cooperativeId) + "/" + importId + "/confirm")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("replay.xlsx", bytes))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void confirm_doesNotOverwriteExistingContribution() throws Exception {
        JsonNode existing = registerMember(cooperativeId, "exist_" + id8(), "Exist", "Member");
        UUID memberId = UUID.fromString(existing.path("userId").asText());
        String username = existing.path("username").asText();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/cooperatives/" + cooperativeId + "/contributions/period")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .param("year", "2022")
                        .param("month", "3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lines": [
                                    {
                                      "memberUserId": "%s",
                                      "paidAmount": 500.0000,
                                      "paymentDate": "2022-03-20",
                                      "paymentReference": "LIVE"
                                    }
                                  ]
                                }
                                """.formatted(memberId)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx(
                                "overwrite.xlsx",
                                Map.of(
                                        "Contributions",
                                        List.of(contributionRow(username, "2022", "3", "10000", "9999", "2022-03-05")))))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        Contribution after = contributionRepository
                .findByCooperativeIdAndMemberUserIdAndYearAndMonth(cooperativeId, memberId, 2022, 3)
                .orElseThrow();
        assertThat(after.getPaidAmount()).isEqualByComparingTo("500.0000");
        assertThat(after.getPaymentReference()).isEqualTo("LIVE");
    }

    @Test
    void historicalData_appearsInReportsAndDashboard_andPayoutsAdjustAvailableFund() throws Exception {
        String username = "hist_rep_" + id8();
        Map<String, List<List<String>>> sheets = validMemberAndContribution(username, "20000");
        sheets.put(
                "Payouts",
                List.of(List.of(
                        "PAY-2022-01",
                        "Year-end",
                        "2022-01-01",
                        "2022-12-31",
                        "2023-01-15",
                        "20000",
                        "20000",
                        "PAID",
                        "")));
        sheets.put(
                "PayoutLines",
                List.of(List.of("PAY-2022-01", username, "20000", "100", "20000", "PAID")));

        MvcResult preview = mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("payout.xlsx", sheets))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andReturn();
        UUID importId = UUID.fromString(objectMapper
                .readTree(preview.getResponse().getContentAsString())
                .path("data")
                .path("importId")
                .asText());
        mockMvc.perform(post(base(cooperativeId) + "/" + importId + "/confirm")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/reports/export")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportType":"CONTRIBUTIONS","fromDate":"2022-01-01","toDate":"2022-12-31"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/reports/export")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportType":"FINANCIAL_LEDGER","fromDate":"2022-01-01","toDate":"2022-12-31"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/reports/export")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reportType":"FULL_FINANCIAL","fromDate":"2022-01-01","toDate":"2022-12-31"}
                                """))
                .andExpect(status().isOk());

        MvcResult dashboard = mockMvc.perform(get("/api/v1/cooperatives/" + cooperativeId + "/dashboard/summary")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(dashboard.getResponse().getContentAsString()).path("data");
        assertThat(new BigDecimal(data.path("regularContributionsTotal").asText())).isEqualByComparingTo("20000");
        assertThat(new BigDecimal(data.path("totalConfirmedPayouts").asText())).isEqualByComparingTo("20000");
        assertThat(new BigDecimal(data.path("availableGroupFunds").asText())).isEqualByComparingTo("0");
        assertThat(payoutLineRepository.findAll()).isNotEmpty();
        assertThat(membershipRepository.countByCooperativeId(cooperativeId)).isGreaterThan(0);
    }

    @Test
    void historyAndCancel_workForPreConfirmOnly() throws Exception {
        String username = "hist_cancel_" + id8();
        MvcResult preview = mockMvc.perform(multipart(base(cooperativeId) + "/preview")
                        .file(xlsx("cancel.xlsx", validMemberAndContribution(username, "1000")))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andReturn();
        UUID importId = UUID.fromString(objectMapper
                .readTree(preview.getResponse().getContentAsString())
                .path("data")
                .path("importId")
                .asText());
        mockMvc.perform(post(base(cooperativeId) + "/" + importId + "/cancel")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        mockMvc.perform(get(base(cooperativeId))
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("CANCELLED"));
    }

    private String base(UUID coopId) {
        return "/api/v1/cooperatives/" + coopId + "/historical-imports";
    }

    private UUID createCooperative(String name) throws Exception {
        MvcResult create = mockMvc.perform(post("/api/v1/cooperatives")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CooperativeTestFixtures.createBody(name)))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(create.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());
    }

    private JsonNode registerMember(UUID coopId, String username, String first, String last) throws Exception {
        MvcResult register = mockMvc.perform(post("/api/v1/cooperatives/" + coopId + "/members")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"%s",
                                  "lastName":"%s",
                                  "username":"%s",
                                  "email":"%s@test.local",
                                  "roleInCooperative":"MEMBER"
                                }
                                """.formatted(first, last, username, username)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(register.getResponse().getContentAsString()).path("data");
    }

    private String registerAndLogin(UUID coopId, String role) throws Exception {
        String username = role.toLowerCase() + "_" + id8();
        MvcResult register = mockMvc.perform(post("/api/v1/cooperatives/" + coopId + "/members")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"%s",
                                  "lastName":"User",
                                  "username":"%s",
                                  "email":"%s@test.local",
                                  "roleInCooperative":"%s"
                                }
                                """.formatted(role, username, username, role)))
                .andExpect(status().isOk())
                .andReturn();
        String password = objectMapper
                .readTree(register.getResponse().getContentAsString())
                .path("data")
                .path("temporaryPassword")
                .asText();
        return login(username, password);
    }

    private String login(String username, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(login.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private static String id8() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, List<List<String>>> validMemberAndContribution(String username, String amount) {
        return validMemberAndContribution(username, amount, "ACTIVE");
    }

    private static Map<String, List<List<String>>> validMemberAndContribution(
            String username, String amount, String status) {
        Map<String, List<List<String>>> sheets = new LinkedHashMap<>();
        sheets.put("Members", List.of(memberRow(username, status)));
        sheets.put("Contributions", List.of(contributionRow(username, "2022", "3", amount, amount, "2022-03-05")));
        return sheets;
    }

    private static List<String> memberRow(String username, String status) {
        return List.of(
                username,
                "Jane",
                "Uwase",
                username + "@test.local",
                "0781234567",
                "",
                "2022-01-15",
                "1",
                status,
                "MEMBER");
    }

    private static List<String> contributionRow(
            String username, String year, String month, String expected, String paid, String date) {
        return List.of(username, year, month, expected, paid, date, "REF-2022-03", "");
    }

    private static List<String> loanRow(String username, String principal, String outstanding, String status) {
        return List.of(
                "L-2022-001",
                username,
                principal,
                principal,
                principal,
                "10",
                "FLAT",
                "10000",
                "6",
                outstanding,
                "0",
                "2022-05-01",
                "2022-05-02",
                "2022-05-10",
                "2022-11-10",
                status,
                "Business");
    }

    private static byte[] workbookWithCustomHeader(String sheetName, List<String> headers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("Instructions").createRow(0).createCell(0).setCellValue("test");
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static MockMultipartFile xlsx(String name, Map<String, List<List<String>>> sheets) throws Exception {
        return xlsx(name, workbookBytes(sheets));
    }

    private static MockMultipartFile xlsx(String name, byte[] bytes) {
        return new MockMultipartFile(
                "file",
                name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bytes);
    }

    private static byte[] workbookBytes(Map<String, List<List<String>>> sheets) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet instructions = workbook.createSheet("Instructions");
            instructions.createRow(0).createCell(0).setCellValue("Historical import test");
            for (Map.Entry<String, List<List<String>>> entry : sheets.entrySet()) {
                Sheet sheet = workbook.createSheet(entry.getKey());
                HistoricalImportSheet type = HistoricalImportSheet.fromSheetName(entry.getKey()).orElse(null);
                Row header = sheet.createRow(0);
                if (type != null) {
                    for (int i = 0; i < type.getHeaders().size(); i++) {
                        header.createCell(i).setCellValue(type.getHeaders().get(i));
                    }
                } else if (!entry.getValue().isEmpty()) {
                    for (int i = 0; i < entry.getValue().get(0).size(); i++) {
                        header.createCell(i).setCellValue("Col" + i);
                    }
                }
                int r = 1;
                for (List<String> values : entry.getValue()) {
                    Row row = sheet.createRow(r++);
                    for (int c = 0; c < values.size(); c++) {
                        row.createCell(c).setCellValue(values.get(c));
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
