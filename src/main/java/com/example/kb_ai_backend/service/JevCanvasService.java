package com.example.kb_ai_backend.service;

import org.springframework.stereotype.Service;
import java.util.*;

/** Situation -> needs -> semantic-only menus. No keyword retrieval or financial actions. */
@Service
public class JevCanvasService {
    private final SituationMenuService situations;
    public JevCanvasService(SituationMenuService situations) {this.situations=situations;}
    public Map<String,Object> compose(String message) {
        var result=situations.compose(message);
        String greeting=switch(result.status()) {
            case "empty" -> "무엇을 도와드릴까요?";
            case "ready" -> "상황에 맞는 업무와 메뉴를 찾았어요.";
            case "partial" -> "찾은 업무를 먼저 보여드릴게요. 일부 업무는 추가 확인이 필요해요.";
            case "needs_clarification" -> "필요한 업무를 판단할 수 있도록 상황을 조금 더 알려주세요.";
            case "no_match" -> "등록된 메뉴에서 이 상황에 맞는 업무를 찾지 못했어요.";
            default -> throw new IllegalStateException("Invalid situation status");
        };
        return Map.of("greeting",greeting,"status",result.status(),
                "menus",result.recommendations().stream().map(SituationMenuService.Recommendation::menu).toList(),
                "recommendations",result.recommendations(),"selection_metrics",result.metrics(),"decision_notes",result.reviews(),"widgets",List.of());
    }
}
