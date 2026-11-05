package rw.terimbere.csams.modules.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static rw.terimbere.csams.testsupport.PaymentScenario.bearer;
import static rw.terimbere.csams.testsupport.PaymentScenario.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rw.terimbere.csams.modules.loan.repository.LoanRepository;
import rw.terimbere.csams.modules.notification.mail.AccountEmailService;
import rw.terimbere.csams.testsupport.PaymentScenario;

/** Members receive their username and a temporary password by email; officers can (re)send them. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberLoginDetailsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private LoanRepository loanRepository;
    @MockBean private AccountEmailService accountEmails;

    @Test
    void anOfficerSendsSignInDetailsAndTheMemberSignsInWithThem() throws Exception {
        when(accountEmails.isReady()).thenReturn(true);
        PaymentScenario p = new PaymentScenario(mockMvc, objectMapper, jdbc, loanRepository, "ACCUMULATING", 1);
        // registering a member already emails their sign-in details
        verify(accountEmails, atLeastOnce()).sendLoginDetails(anyString(), anyString(), anyString(), anyString(), any());

        // one member has never signed in (as after an import)
        jdbc.update("update users set last_login_at = null where id = ?", p.scheme.memberB().id());
        String path = p.base() + "/members/login-details";
        JsonNode preview = p.data(mockMvc.perform(get(path).header("Authorization", bearer(p.scheme.president()))).andReturn());
        assertThat(preview.path("emailReady").asBoolean()).isTrue();
        assertThat(preview.path("neverSignedIn").asInt()).isEqualTo(1);

        assertThat(status(mockMvc.perform(post(path).header("Authorization", bearer(p.payer))).andReturn())).isEqualTo(403);

        MvcResult sent = mockMvc.perform(post(path).header("Authorization", bearer(p.scheme.president()))
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of())))
                .andReturn();
        assertThat(status(sent)).as(sent.getResponse().getContentAsString()).isEqualTo(200);
        JsonNode r = p.data(sent);
        assertThat(r.path("sent").asInt()).isEqualTo(1);
        assertThat(r.path("sentTo").get(0).asText()).isEqualTo(p.scheme.memberB().username());

        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        // once when the member was registered, once now
        verify(accountEmails, org.mockito.Mockito.times(2))
                .sendLoginDetails(anyString(), anyString(), eq(p.scheme.memberB().username()), password.capture(), any());
        assertThat(password.getValue()).matches("[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}");
        assertThat(p.fixtures.login(p.scheme.memberB().username(), password.getValue())).isNotBlank();

        // picked by name, a member who already signs in can be sent new details too
        MvcResult picked = mockMvc.perform(post(path).header("Authorization", bearer(p.scheme.president()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("memberUserIds", List.of(p.payer.id())))))
                .andReturn();
        assertThat(p.data(picked).path("sent").asInt()).isEqualTo(1);
    }

    @Test
    void nothingIsChangedWhenEmailIsNotSetUp() throws Exception {
        when(accountEmails.isReady()).thenReturn(false);
        PaymentScenario p = new PaymentScenario(mockMvc, objectMapper, jdbc, loanRepository, "ACCUMULATING", 1);
        MvcResult refused = mockMvc.perform(post(p.base() + "/members/login-details").header("Authorization", bearer(p.scheme.president())))
                .andReturn();
        assertThat(status(refused)).isEqualTo(422);
        assertThat(p.code(refused)).isEqualTo("EMAIL_NOT_CONFIGURED");
    }
}
