package com.example.kb_ai_backend.controller;

import com.example.kb_ai_backend.service.JevCanvasService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CanvasControllerTest {
    @Test void retainsFrontendContractAndReportsUpstreamFailure() throws Exception {
        var service = mock(JevCanvasService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CanvasController(service)).build();
        when(service.compose("환전")).thenReturn(Map.of("greeting", "필요한 기능을 모았어요.", "widgets", List.of(Map.of("type", "travel_card_charge_widget")), "status", "ready"));
        mvc.perform(post("/api/canvas").contentType("application/json").content("{\"message\":\"환전\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.greeting").isString())
                .andExpect(jsonPath("$.widgets[0].type").value("travel_card_charge_widget"));
        when(service.compose("오류")).thenThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "시간 초과"));
        mvc.perform(post("/api/canvas").contentType("application/json").content("{\"message\":\"오류\"}"))
                .andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.error").value("시간 초과"));
    }
}
