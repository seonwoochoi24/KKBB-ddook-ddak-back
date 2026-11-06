package com.example.kb_ai_backend.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

/** Exactly one widget-selection call. Explanations never gate the response. */
@Service
public class JevCanvasService {
    private final WidgetSelectionExperimentService selector;
    private final CanvasParameters parameters;
    private final WidgetDataService data;

    public JevCanvasService(WidgetSelectionExperimentService selector, CanvasParameters parameters, WidgetDataService data) {
        this.selector = selector; this.parameters = parameters; this.data = data;
    }

    public Map<String, Object> compose(String message) {
        if (message == null || message.isBlank()) return response("무엇을 도와드릴까요?", "empty", List.of());
        if (message.length() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message는 2000자 이내로 입력하세요.");
        var selection = selector.select(message, "jev");
        var ids = selection.widgets();
        if (ids == null || ids.size() > 3 || new HashSet<>(ids).size() != ids.size()
                || ids.stream().anyMatch(id -> !WidgetSelectionExperimentService.WIDGETS.containsKey(id)))
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "위젯 선택 결과가 올바르지 않습니다.");
        if (selection.needsReview()) return response("필요한 금융 기능을 조금 더 구체적으로 알려주세요.", "needs_clarification", List.of());
        if (ids.isEmpty()) return response("여행 준비, 모임 회비 관리, 분실 대응 중 필요한 기능을 알려주세요.", "unsupported", List.of());
        boolean travel = ids.stream().anyMatch(id -> Set.of("travel_card_charge_widget", "overseas_qr_payment_widget", "travel_insurance_widget").contains(id));
        var values = travel ? parameters.extract(message) : new CanvasParameters.Values(null, null, null, null, false);
        if (values.needsReview()) return response("여행지·통화·금액·기간을 하나씩 명확하게 입력해주세요.", "needs_clarification", List.of());
        if (ids.contains("travel_card_charge_widget") && (values.currency() == null || values.currency().equals("KRW")))
            return response("환전할 통화를 함께 입력해주세요. 예: 일본 여행 2만엔 환전", "needs_clarification", List.of());
        var widgets = ids.stream().map(id -> data.buildWidget(id, values.currency(), values.destination(), values.amount(), null, values.tripDays())).toList();
        return response("필요한 기능을 모았어요.", "ready", widgets);
    }

    private Map<String, Object> response(String greeting, String status, List<?> widgets) {
        return Map.of("greeting", greeting, "status", status, "widgets", widgets);
    }
}
