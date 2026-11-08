package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class JevMenuSelectionServiceTest {
    ObjectMapper mapper = new ObjectMapper();
    RestTemplate client = new RestTemplate();
    MockRestServiceServer server;
    MenuCatalog catalog;
    JevMenuSelectionService service;
    @BeforeEach void setup() throws Exception {
        catalog = new MenuCatalog(mapper);
        server = MockRestServiceServer.createServer(client);
        service = new JevMenuSelectionService(client, catalog, "test-key", "jev-latest", 0.6);
    }
    String answer(String choice, Object confidence) throws Exception {
        return mapper.writeValueAsString(Map.of("answers", Map.of("menu", Map.of(
                "type", "choice", "choice", choice, "confidence", confidence))));
    }
    void expectAnswer(String choice, Object confidence) throws Exception {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andRespond(withSuccess(answer(choice, confidence), MediaType.APPLICATION_JSON));
    }
    @Test void retrievesCandidatesAndSelectsOriginalMenuWithOneCall() throws Exception {
        var menu = catalog.all().stream().filter(m -> m.name().equals("휴양림 예약서비스")).findFirst().orElseThrow();
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone"))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(request -> {
                    var body = mapper.readTree(((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString());
                    assertEquals(40, body.path("state").path("menu_catalog").size());
                    assertEquals(1, body.path("questions").size());
                    assertEquals(42, body.path("questions").path("menu").path("criteria").size());
                    assertEquals("휴양림 예약하고 싶어", body.path("state").path("user_request").asText());
                }).andRespond(withSuccess(answer(menu.id(), 0.95), MediaType.APPLICATION_JSON));
        var result = service.select("휴양림 예약하고 싶어");
        assertEquals("selected", result.status()); assertEquals(menu, result.menu()); server.verify();
        assertEquals(628, catalog.all().size());
        assertNotNull(mapper.valueToTree(menu).get("select_when"));
    }
    @Test void retrievalKeepsPredeclaredExpectedMenus() throws Exception {
        try (var input = getClass().getResourceAsStream("/menu-selection-cases.json")) {
            for (var row : mapper.readTree(input).path("cases")) {
                var ids = catalog.candidates(row.path("message").asText(), 40).stream().map(MenuCatalog.Menu::id).toList();
                for (var id : row.path("expected_menu_ids"))
                    assertTrue(ids.contains(id.asText()), row.path("id").asText() + " missing " + id.asText());
            }
        }
    }
    @Test void rejectsInventedIdEvenAtLowConfidence() throws Exception {
        expectAnswer("invented_menu", 0.1);
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.select("환전")).getStatusCode().value());
        server.verify();
    }
    @Test void rejectsCatalogIdOutsideRetrievedCandidates() throws Exception {
        var candidates = catalog.candidates("환전", 40);
        expectAnswer(catalog.all().stream().filter(m -> !candidates.contains(m)).findFirst().orElseThrow().id(), 0.95);
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.select("환전")).getStatusCode().value()); server.verify();
    }
    @Test void lowConfidenceSuppressesSelectedMenu() throws Exception {
        expectAnswer(catalog.candidates("아무거나", 40).get(0).id(), 0.2);
        var result = service.select("아무거나"); assertEquals("needs_clarification", result.status());
        assertNull(result.menu()); server.verify();
    }
    @Test void handlesNoMatchAndAmbiguity() throws Exception {
        expectAnswer("no_match", 0.95); expectAnswer("unsure", 0.9);
        assertEquals("no_match", service.select("날씨").status());
        assertEquals("needs_clarification", service.select("조회해줘").status()); server.verify();
    }
    @Test void rejectsMalformedConfidence() throws Exception {
        expectAnswer("no_match", "0.9");
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.select("날씨")).getStatusCode().value());
        server.verify();
    }
    @Test void rejectsInputAndMissingKeyBeforeNetwork() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.select(" ")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.select("가".repeat(2001))).getStatusCode().value());
        var noKey = new JevMenuSelectionService(client, catalog, "", "jev-latest", 0.6);
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> noKey.select("환전")).getStatusCode().value()); server.verify();
    }
    @Test void preservesUpstreamError() {
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone")).andRespond(withUnauthorizedRequest());
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.select("환전")).getStatusCode().value()); server.verify();
    }
}
