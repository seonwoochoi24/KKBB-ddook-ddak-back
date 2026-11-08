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
    @Test void returnsMenuSelectionAndReportsUpstreamFailure() throws Exception {
        var service = mock(JevCanvasService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new CanvasController(service)).build();
        when(service.compose("환전")).thenReturn(Map.of("greeting", "필요한 기능을 모았어요.", "menus", List.of(Map.of("id", "kb_menu_0001", "name", "인기 상품")), "widgets", List.of(), "status", "ready"));
        mvc.perform(post("/api/canvas").contentType("application/json").content("{\"message\":\"환전\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.greeting").isString())
                .andExpect(jsonPath("$.menus[0].id").value("kb_menu_0001")).andExpect(jsonPath("$.widgets").isEmpty());
        when(service.compose("오류")).thenThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "시간 초과"));
        mvc.perform(post("/api/canvas").contentType("application/json").content("{\"message\":\"오류\"}"))
                .andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.error").value("시간 초과"));
    }
}
