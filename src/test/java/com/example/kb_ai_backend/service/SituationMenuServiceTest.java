package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SituationMenuServiceTest {
    JevDecisionClient jev=mock(JevDecisionClient.class);
    SemanticMenuRetriever semantic=mock(SemanticMenuRetriever.class);
    MenuCatalog catalog;
    SituationMenuService service;
    @BeforeEach void setup() throws Exception {
        var mapper=new ObjectMapper();catalog=new MenuCatalog(mapper);service=new SituationMenuService(jev,semantic,catalog,mapper,.6);
    }
    JevDecisionClient.Choice choice(String id){return new JevDecisionClient.Choice(id,.95);}
    JevDecisionClient.Decisions plan(String... ids) {
        var answers=new LinkedHashMap<String,JevDecisionClient.Choice>();for(int i=0;i<3;i++)answers.put("need_"+(i+1),choice(i<ids.length?ids[i]:"none"));
        return new JevDecisionClient.Decisions(answers,100);
    }
    MenuCatalog.Menu menu(String name){return catalog.all().stream().filter(m->m.name().equals(name)).findFirst().orElseThrow();}
    @Test void interpretsSituationBeforeSemanticSearchAndReturnsMultipleSuggestions() {
        var exchange=menu("환전신청");var payment=menu("해외 QR 결제");var insurance=menu("KB손해보험 다이렉트 해외여행보험");
        when(semantic.retrieve("환전신청",12)).thenReturn(List.of(exchange));
        when(semantic.retrieve("해외 QR 결제",12)).thenReturn(List.of(payment));
        when(semantic.retrieve("KB손해보험 다이렉트 해외여행보험",12)).thenReturn(List.of(insurance));
        var answers=new LinkedHashMap<String,JevDecisionClient.Choice>();int i=1;
        for(var m:List.of(exchange,payment,insurance)){answers.put("task_"+i,choice(m.id()));answers.put("task_"+i+"_basis",choice("suggested"));i++;}
        when(jev.decide(anyMap(),anyMap())).thenReturn(plan("foreign_exchange","foreign_payment","travel_insurance"),new JevDecisionClient.Decisions(answers,130));
        var result=service.compose("다음 달 일본으로 처음 여행 가. 뭘 준비할까?");
        assertEquals("ready",result.status());assertEquals(3,result.recommendations().size());
        assertTrue(result.recommendations().stream().allMatch(r->r.basis().equals("suggested")));
        var order=inOrder(jev,semantic);order.verify(jev).decide(anyMap(),anyMap());order.verify(semantic).retrieve("환전신청",12);
        verify(jev,times(2)).decide(anyMap(),anyMap());assertEquals("semantic_only",result.metrics().retrievalMode());
    }
    @Test void searchesOnlyPlannedNeedAndPreservesExplicitBasis() {
        var m=menu("환전신청");when(semantic.retrieve("환전신청",40)).thenReturn(List.of(m));
        when(jev.decide(anyMap(),anyMap())).thenReturn(plan("foreign_exchange"),new JevDecisionClient.Decisions(Map.of("task_1",choice(m.id()),"task_1_basis",choice("explicit")),100));
        var result=service.compose("보험은 이미 가입했고 환전만 할래");assertEquals("explicit",result.recommendations().get(0).basis());
        verify(semantic).retrieve("환전신청",40);verifyNoMoreInteractions(semantic);
    }
    @Test void ambiguousOrUnrelatedSituationDoesNotSearchOrSelectMenus() {
        when(jev.decide(anyMap(),anyMap())).thenReturn(plan("unsure"),plan("no_match"));
        assertEquals("needs_clarification",service.compose("도와줘").status());assertEquals("no_match",service.compose("요리법").status());verifyNoInteractions(semantic);
    }
    @Test void blankAndTooLongInputsDoNotCallJev() {
        assertEquals("empty",service.compose(" ").status());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->service.compose("가".repeat(2001))).getStatusCode().value());verifyNoInteractions(jev,semantic);
    }
    @Test void semanticFailureNeverFallsBackToKeywords() {
        when(jev.decide(anyMap(),anyMap())).thenReturn(plan("account_proof"));
        when(semantic.retrieve(anyString(),anyInt())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        assertEquals(503,assertThrows(ResponseStatusException.class,()->service.compose("회사 제출용 계좌 증빙")).getStatusCode().value());verify(jev,times(1)).decide(anyMap(),anyMap());
    }
    @Test void multipleUnresolvedNeedsDoNotForceOneArbitraryMenu() {
        var m=menu("환전신청");when(semantic.retrieve("환전신청",40)).thenReturn(List.of(m));
        when(jev.decide(anyMap(),anyMap())).thenReturn(plan("foreign_exchange"),new JevDecisionClient.Decisions(Map.of("task_1",choice("unsure"),"task_1_basis",choice("explicit")),100));
        var result=service.compose("외화 준비");assertEquals("needs_clarification",result.status());assertTrue(result.recommendations().isEmpty());
    }    @Test void uncertainRequestBasisDoesNotDiscardConfidentBusinessSupportMenu() {
        var m=menu("정책자금 맞춤추천");
        when(semantic.retrieve("정책자금 맞춤추천",40)).thenReturn(List.of(m));
        when(jev.decide(anyMap(),anyMap())).thenReturn(plan("business_funding"),new JevDecisionClient.Decisions(Map.of(
                "task_1",new JevDecisionClient.Choice(m.id(),1.0),
                "task_1_basis",new JevDecisionClient.Choice("explicit",.32)),245));
        var result=service.compose("나 사장인데 지원금 받고 싶어");
        assertEquals("ready",result.status());assertEquals(m,result.recommendations().get(0).menu());
        assertEquals("uncertain",result.recommendations().get(0).basis());
        assertEquals(1.0,result.recommendations().get(0).menuConfidence());
        assertEquals(.32,result.recommendations().get(0).basisConfidence());
        assertEquals("basis_uncertain",result.reviews().get(0).reason());
    }
    @Test void uncertainMenuStillRequiresClarificationEvenWhenBasisIsCertain() {
        var m=menu("정책자금 맞춤추천");when(semantic.retrieve("정책자금 맞춤추천",40)).thenReturn(List.of(m));
        when(jev.decide(anyMap(),anyMap())).thenReturn(plan("business_funding"),new JevDecisionClient.Decisions(Map.of(
                "task_1",new JevDecisionClient.Choice(m.id(),.3),"task_1_basis",choice("explicit")),100));
        var result=service.compose("지원금 받고 싶어");assertEquals("needs_clarification",result.status());
        assertTrue(result.recommendations().isEmpty());assertEquals("low_menu_confidence",result.reviews().get(0).reason());
    }
    @Test void lowConfidenceExtraSuggestionDoesNotVetoPrimaryNeed() {
        var m=menu("정책자금 맞춤추천");when(semantic.retrieve("정책자금 맞춤추천",40)).thenReturn(List.of(m));
        var planning=new JevDecisionClient.Decisions(Map.of(
                "need_1",new JevDecisionClient.Choice("business_funding",.99),
                "need_2",new JevDecisionClient.Choice("business_consulting",.26),
                "need_3",new JevDecisionClient.Choice("business_funding",.38)),431);
        when(jev.decide(anyMap(),anyMap())).thenReturn(planning,new JevDecisionClient.Decisions(Map.of("task_1",choice(m.id()),"task_1_basis",choice("explicit")),200));
        var result=service.compose("나 사장인데 지원금 받고 싶어");assertEquals("ready",result.status());
        assertEquals(1,result.recommendations().size());assertEquals(m,result.recommendations().get(0).menu());
        assertEquals("optional_need_omitted",result.reviews().get(0).reason());verifyNoMoreInteractionsExceptFunding();
    }
    private void verifyNoMoreInteractionsExceptFunding() {
        verify(semantic).retrieve("정책자금 맞춤추천",40);verifyNoMoreInteractions(semantic);
    }

}
