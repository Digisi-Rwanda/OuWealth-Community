package rw.terimbere.csams.modules.member;

import rw.terimbere.csams.modules.cooperative.CooperativeTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import rw.terimbere.csams.modules.user.entity.AccountStatus;
import rw.terimbere.csams.modules.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String superAdminToken;
    private UUID cooperativeId;

    @BeforeEach
    void setUp() throws Exception {
        superAdminToken = loginAccessToken("superadmin", "ChangeMe@123!");

        String name = "Member Test Coop " + UUID.randomUUID().toString().substring(0, 8);
        MvcResult create = mockMvc.perform(post("/api/v1/cooperatives")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CooperativeTestFixtures.createBody(name)))
                .andExpect(status().isOk())
                .andReturn();
        cooperativeId = UUID.fromString(objectMapper
                .readTree(create.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());
    }

    @Test
    void registerListSuspend_andDuplicateUsernameConflict() throws Exception {
        String username = "member_" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@test.local";

        MvcResult register = mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/members")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Jane",
                                  "lastName":"Doe",
                                  "username":"%s",
                                  "email":"%s",
                                  "phone":"+250780000001",
                                  "roleInCooperative":"MEMBER"
                                }
                                """.formatted(username, email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.membershipStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.shareCount").value(0))
                .andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty())
                .andExpect(jsonPath("$.data.membershipDate").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(register.getResponse().getContentAsString()).path("data");
        UUID memberUserId = UUID.fromString(data.path("userId").asText());
        String tempPassword = data.path("temporaryPassword").asText();
        assertThat(tempPassword).hasSizeGreaterThanOrEqualTo(8);

        mockMvc.perform(get("/api/v1/cooperatives/" + cooperativeId + "/members")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .param("q", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(memberUserId.toString()));

        mockMvc.perform(get("/api/v1/cooperatives/" + cooperativeId + "/members/" + memberUserId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.userId").value(memberUserId.toString()))
                .andExpect(jsonPath("$.data.contributions").isArray())
                .andExpect(jsonPath("$.data.loans").isArray());

        mockMvc.perform(patch("/api/v1/cooperatives/" + cooperativeId + "/members/" + memberUserId + "/status")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountStatus":"SUSPENDED","membershipStatus":"SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.data.membershipStatus").value("SUSPENDED"));

        assertThat(userRepository.findByIdAndDeletedFalse(memberUserId).orElseThrow().getAccountStatus())
                .isEqualTo(AccountStatus.SUSPENDED);

        mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/members")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Dup",
                                  "lastName":"User",
                                  "username":"%s",
                                  "email":"other_%s@test.local"
                                }
                                """.formatted(username, UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void assignAdministrator_createsAdminUser() throws Exception {
        String username = "admin_" + UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/administrators")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"%s",
                                  "email":"%s@test.local",
                                  "firstName":"Admin",
                                  "lastName":"User"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleInCooperative").value("PRESIDENT"))
                .andExpect(jsonPath("$.data.temporaryPassword").isNotEmpty());
    }

    @Test
    void updateMember_canChangeUsername() throws Exception {
        String username = "upd_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult register = mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/members")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Jane",
                                  "lastName":"Doe",
                                  "username":"%s",
                                  "email":"%s@test.local"
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andReturn();
        UUID memberUserId = UUID.fromString(objectMapper
                .readTree(register.getResponse().getContentAsString())
                .path("data")
                .path("userId")
                .asText());
        String newUsername = username + "2";

        mockMvc.perform(put("/api/v1/cooperatives/" + cooperativeId + "/members/" + memberUserId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Jane",
                                  "lastName":"Doe",
                                  "username":"%s",
                                  "email":"%s@test.local"
                                }
                                """.formatted(newUsername, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(newUsername));
    }

    @Test
    void registerAndUpdateIgnoreShareCount() throws Exception {
        String username = "shares_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult register = mockMvc.perform(post("/api/v1/cooperatives/" + cooperativeId + "/members")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Share",
                                  "lastName":"Locked",
                                  "username":"%s",
                                  "email":"%s@test.local",
                                  "shareCount":5
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareCount").value(0))
                .andReturn();
        UUID memberUserId = UUID.fromString(objectMapper
                .readTree(register.getResponse().getContentAsString())
                .path("data")
                .path("userId")
                .asText());

        mockMvc.perform(put("/api/v1/cooperatives/" + cooperativeId + "/members/" + memberUserId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Share",
                                  "lastName":"Locked",
                                  "username":"%s",
                                  "email":"%s@test.local",
                                  "shareCount":12
                                }
                                """.formatted(username, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareCount").value(0));
    }

    private String loginAccessToken(String username, String password) throws Exception {
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
}
