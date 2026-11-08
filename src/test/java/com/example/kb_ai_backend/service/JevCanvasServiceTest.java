package com.example.kb_ai_backend.service;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JevCanvasServiceTest {
    @Test void returnsSuggestedWorkAndExistingMenu() {
        var situations=mock(SituationMenuService.class);var menu=new MenuCatalog.Menu("kb_menu_0323","환전신청","환전",List.of("외화 준비"));
        var rec=new SituationMenuService.Recommendation(menu,"foreign_exchange","현지 통화 준비","suggested","상황에 따라 검토할 수 있는 업무");
        when(situations.compose("여행 준비")).thenReturn(new SituationMenuService.Result("ready",List.of(rec),new SituationMenuService.Metrics("semantic_only",100,10,150,2)));
        var response=new JevCanvasService(situations).compose("여행 준비");
        assertEquals("ready",response.get("status"));assertEquals(List.of(menu),response.get("menus"));assertEquals(List.of(rec),response.get("recommendations"));
        assertEquals(List.of(),response.get("widgets"));verify(situations).compose("여행 준비");
    }
}
