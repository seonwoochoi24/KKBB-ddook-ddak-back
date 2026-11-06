package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WidgetSelectionExperimentServiceTest {
    final ObjectMapper mapper = new ObjectMapper();
    RestTemplate client;
    MockRestServiceServer server;
    WidgetSelectionExperimentService service;
    @BeforeEach void setup() {
        client = new RestTemplate();
        server = MockRestServiceServer.createServer(client);
        service = new WidgetSelectionExperimentService(client, mapper, "test-jev", "test-openai", "jev-latest", 0.6);
    }
    String jevResponse(Set<String> selected, double confidence) throws Exception {
        var answers = new LinkedHashMap<String, Object>();
        WidgetSelectionExperimentService.WIDGETS.keySet().forEach(id -> answers.put(id,
                Map.of("type", "choice", "choice", selected.contains(id) ? "include" : "exclude", "confidence", confidence)));
        return mapper.writeValueAsString(Map.of("model", "jev-test", "answers", answers,
                "usage", Map.of("input_tokens", 100, "output_tokens", 30)));
    }
    @Test void supportsMultipleWidgetsInOneJevCall() throws Exception {
        var selected = Set.of("travel_card_charge_widget", "travel_insurance_widget");
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andExpect(header("Authorization", "Bearer test-jev"))
                .andExpect(jsonPath("$.questions.travel_card_charge_widget.type").value("choice"))
                .andRespond(withSuccess(jevResponse(selected, 0.95), MediaType.APPLICATION_JSON));
        var result = service.select("상하이 환전과 보험", "jev");
        assertEquals(selected, new HashSet<>(result.widgets()));
        assertEquals("jev-test", result.model()); assertEquals(100L, result.inputTokens());
        assertFalse(result.needsReview()); server.verify();
    }
    @Test void lowConfidenceIsVisibleInsteadOfSilentlyActing() throws Exception {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess(jevResponse(Set.of(), 0.2), MediaType.APPLICATION_JSON));
        assertTrue(service.select("뭐 해야 하지", "jev").needsReview()); server.verify();
    }
    @Test void malformedUpstreamResponseIsNotSuccessfulEmptySelection() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess("{\"answers\":{}}", MediaType.APPLICATION_JSON));
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> service.select("환전", "jev")).getStatusCode().value()); server.verify();
    }
    @Test void authenticationFailureIsNotHidden() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        var e = assertThrows(ResponseStatusException.class, () -> service.select("환전", "jev"));
        assertEquals(502, e.getStatusCode().value()); assertTrue(e.getReason().contains("401")); server.verify();
    }
    @Test void missingKeyDoesNotBreakStartupButCannotCallProvider() {
        var noKey = new WidgetSelectionExperimentService(client, mapper, "", "", "jev-latest", 0.6);
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> noKey.select("환전", "jev")).getStatusCode().value()); server.verify();
    }
    @Test void rejectsBlankAndUnknownProviderBeforeNetworkCall() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.select(" ", "jev")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.select("환전", "other")).getStatusCode().value()); server.verify();
    }
    @Test void openaiComparisonDoesOnlyTheSameWidgetTask() throws Exception {
        var decisions = new LinkedHashMap<String, Object>();
        WidgetSelectionExperimentService.WIDGETS.keySet().forEach(id -> decisions.put(id,
                id.equals("travel_card_charge_widget") ? "include" : "exclude"));
        var response = Map.of("model", "gpt-4o-mini-test", "usage", Map.of("prompt_tokens", 200, "completion_tokens", 40),
                "choices", List.of(Map.of("message", Map.of("content", mapper.writeValueAsString(decisions)))));
        server.expect(requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andRespond(withSuccess(mapper.writeValueAsString(response), MediaType.APPLICATION_JSON));
        var result = service.select("환전", "openai");
        assertEquals(List.of("travel_card_charge_widget"), result.widgets());
        assertNull(result.decisions().get("travel_card_charge_widget").confidence());
        assertEquals("widget_selection_only", result.scope()); server.verify();
    }
}
