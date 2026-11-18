package com.retailpos.server.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.retailpos.server.PosIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** When Electron sets a launch token, /api requires it; health and static UI do not. */
@PosIntegrationTest
@TestPropertySource(properties = "pos.launch-token=test-launch-token")
@AutoConfigureMockMvc
class LaunchTokenTest {

    @Autowired MockMvc mvc;

    @Test
    void apiRejectsMissingOrWrongToken() throws Exception {
        mvc.perform(get("/api/setup/status")).andExpect(status().isForbidden());
        mvc.perform(get("/api/setup/status").header("X-POS-Launch-Token", "wrong")).andExpect(status().isForbidden());
    }

    @Test
    void apiAcceptsCorrectToken() throws Exception {
        mvc.perform(get("/api/setup/status").header("X-POS-Launch-Token", "test-launch-token")).andExpect(status().isOk());
    }

    @Test
    void healthDoesNotNeedToken() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
