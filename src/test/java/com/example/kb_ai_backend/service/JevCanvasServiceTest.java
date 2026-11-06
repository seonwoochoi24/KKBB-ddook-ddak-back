package com.example.kb_ai_backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JevCanvasServiceTest {
    WidgetSelectionExperimentService selector = mock(WidgetSelectionExperimentService.class);
    WidgetDataService data = mock(WidgetDataService.class);
    JevCanvasService service;
    @BeforeEach void setup() { service = new JevCanvasService(selector, new CanvasParameters(), data); }
    void selected(String message, List<String> ids, boolean review) {
        when(selector.select(message, "jev")).thenReturn(new WidgetSelectionExperimentService.Result(
                "jev", "test", "widget_selection_only", ids, Map.of(), review, 200, 100L, 30L));
    }
    @Test void rendersWithOneSelectionCallAndLocalTravelParameters() {
        String message = "일본 3박4일 여행 2만엔 환전과 보험";
        var ids = List.of("travel_card_charge_widget", "travel_insurance_widget");
        selected(message, ids, false);
        for (String id : ids) when(data.buildWidget(id, "JPY", "일본", 20000, null, 4)).thenReturn(Map.of("type", id));
        var response = service.compose(message);
        assertEquals("ready", response.get("status"));
        assertEquals("필요한 기능을 모았어요.", response.get("greeting"));
        assertEquals(2, ((List<?>) response.get("widgets")).size());
        verify(selector, times(1)).select(message, "jev"); verifyNoMoreInteractions(selector);
        for (String id : ids) verify(data).buildWidget(id, "JPY", "일본", 20000, null, 4);
        verifyNoMoreInteractions(data);
    }
    @Test void blankInputDoesNotCallAnyProvider() {
        assertEquals(List.of(), service.compose(" ").get("widgets")); verifyNoInteractions(selector, data);
    }
    @Test void oversizedInputIsRejectedBeforeProviderCall() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.compose("가".repeat(2001))).getStatusCode().value());
        verifyNoInteractions(selector, data);
    }
    @Test void uncertaintyDoesNotConstructFinancialWidgets() {
        selected("아무거나", List.of("card_freeze_all"), true);
        assertEquals("needs_clarification", service.compose("아무거나").get("status")); verifyNoInteractions(data);
    }
    @Test void validatesAllowlistAndBudgetBeforeBuilding() {
        for (var ids : List.of(List.of("unknown"), List.of("card_freeze_all", "card_freeze_all"),
                List.of("card_freeze_all", "atm_smart_withdrawal", "id_reissue_status_widget", "travel_insurance_widget"))) {
            selected("요청", ids, false);
            assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.compose("요청")).getStatusCode().value());
        }
        verifyNoInteractions(data);
    }
    @Test void unknownCurrencyAsksForClarificationWithoutFetchingRates() {
        selected("여행 환전", List.of("travel_card_charge_widget"), false);
        assertEquals("needs_clarification", service.compose("여행 환전").get("status")); verifyNoInteractions(data);
    }
    @Test void unsupportedRequestAndConflictingAmountsNeverBuildWidgets() {
        selected("날씨", List.of(), false);
        assertEquals("unsupported", service.compose("날씨").get("status"));
        String message = "일본 100엔 아니 200엔 환전";
        selected(message, List.of("travel_card_charge_widget"), false);
        assertEquals("needs_clarification", service.compose(message).get("status")); verifyNoInteractions(data);
    }
    @Test void upstreamFailureIsPreservedInsteadOfPretendingThereAreNoWidgets() {
        when(selector.select("환전", "jev")).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals(503, assertThrows(ResponseStatusException.class, () -> service.compose("환전")).getStatusCode().value());
        verifyNoInteractions(data);
    }
}
