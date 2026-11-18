package com.retailpos.server.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.retailpos.server.PosIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@PosIntegrationTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;   // JPA writes are flushed before JDBC asserts read them

    @Test
    void firstRunCreatesOwnerOnlyOnce() throws Exception {
        mvc.perform(get("/api/setup/status")).andExpect(status().isOk()).andExpect(jsonPath("$.needsOwner").value(true));

        createOwner().andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("OWNER"))
            .andExpect(jsonPath("$.permissions[?(@ == 'USER_MANAGE')]").exists());

        mvc.perform(get("/api/setup/status")).andExpect(jsonPath("$.needsOwner").value(false));
        createOwner().andExpect(status().isConflict());
    }

    @Test
    void pinIsStoredHashed() throws Exception {
        createOwner();
        em.flush();
        String hash = jdbc.queryForObject("SELECT pin_hash FROM app_user WHERE username = 'owner'", String.class);
        assertThat(hash).isNotEqualTo("1234").startsWith("$2");
    }

    @Test
    void loginMeLogout() throws Exception {
        createOwner();
        mvc.perform(get("/api/auth/users")).andExpect(jsonPath("$[0].displayName").value("Sam Owner"));

        String body = login("owner", "1234").andExpect(status().isOk())
            .andExpect(jsonPath("$.user.displayName").value("Sam Owner"))
            .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("owner"));

        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void usernameIsCaseInsensitive() throws Exception {
        createOwner();
        login("OWNER", "1234").andExpect(status().isOk());
    }

    @Test
    void protectedApiNeedsSession() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-session")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPinAndUnknownUserGiveSameError() throws Exception {
        createOwner();
        login("owner", "9999").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Incorrect user or PIN."));
        login("nobody", "9999").andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Incorrect user or PIN."));
        em.flush();
        Integer fails = jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = 'LOGIN_FAIL'", Integer.class);
        assertThat(fails).isEqualTo(2);
    }

    @Test
    void locksAfterFiveWrongPins() throws Exception {
        createOwner();
        for (int i = 1; i < AuthService.MAX_ATTEMPTS; i++) {
            login("owner", "0000").andExpect(status().isUnauthorized());
        }
        login("owner", "0000").andExpect(status().isLocked());
        login("owner", "1234").andExpect(status().isLocked());      // correct PIN still refused while locked
    }

    @Test
    void successResetsFailureCount() throws Exception {
        createOwner();
        login("owner", "0000").andExpect(status().isUnauthorized());
        login("owner", "1234").andExpect(status().isOk());
        em.flush();
        assertThat(jdbc.queryForObject("SELECT failed_attempts FROM app_user WHERE username = 'owner'", Integer.class)).isZero();
    }

    @Test
    void rejectsBadPinFormat() throws Exception {
        login("owner", "12").andExpect(status().isBadRequest());
        login("owner", "12ab").andExpect(status().isBadRequest());
        mvc.perform(post("/api/setup/owner").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"owner\",\"displayName\":\"Sam\",\"pin\":\"1\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void otherPathsStayDenied() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
    }

    private org.springframework.test.web.servlet.ResultActions createOwner() throws Exception {
        return mvc.perform(post("/api/setup/owner").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"owner\",\"displayName\":\"Sam Owner\",\"pin\":\"1234\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String user, String pin) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + user + "\",\"pin\":\"" + pin + "\"}"));
    }
}
