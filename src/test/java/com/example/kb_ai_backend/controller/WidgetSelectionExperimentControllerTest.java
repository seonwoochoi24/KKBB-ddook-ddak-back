package com.example.kb_ai_backend.controller;

import com.example.kb_ai_backend.service.WidgetSelectionExperimentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WidgetSelectionExperimentControllerTest {
    @Test void providerFailureHasVisibleErrorInsteadOfEmptyWidgets() throws Exception {
        var service = mock(WidgetSelectionExperimentService.class);
        when(service.select("환전", "jev")).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_GATEWAY, "jev API 오류: HTTP 401"));
        var mvc = MockMvcBuilders.standaloneSetup(new WidgetSelectionExperimentController(service)).build();
        mvc.perform(post("/api/experiments/widget-selection")
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"환전\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("jev API 오류: HTTP 401"))
                .andExpect(jsonPath("$.widgets").doesNotExist());
    }
}
