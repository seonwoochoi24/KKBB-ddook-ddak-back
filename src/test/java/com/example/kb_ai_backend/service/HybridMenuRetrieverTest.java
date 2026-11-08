package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HybridMenuRetrieverTest {
    ObjectMapper mapper=new ObjectMapper();
    @Test void fusesSemanticResultsAndBoundsCandidateCount() throws Exception {
        var catalog=new MenuCatalog(mapper);var client=new RestTemplate();var server=MockRestServiceServer.createServer(client);
        var lexical=catalog.candidates("회사에 낼 증빙",80);
        var semantic=new ArrayList<>(catalog.all().stream().map(MenuCatalog.Menu::id).toList());Collections.reverse(semantic);
        var semanticOnly=semantic.stream().filter(id->lexical.stream().noneMatch(m->m.id().equals(id))).findFirst().orElseThrow();
        var ranking=new ArrayList<String>();ranking.add(semanticOnly);
        ranking.addAll(lexical.stream().limit(79).map(MenuCatalog.Menu::id).toList());
        server.expect(requestTo("http://127.0.0.1:18083/search"))
                .andExpect(jsonPath("$.catalog_sha256").value(catalog.sha256()))
                .andRespond(withSuccess(mapper.writeValueAsString(Map.of("catalog_sha256",catalog.sha256(),"ids",ranking)),MediaType.APPLICATION_JSON));
        var result=new HybridMenuRetriever(catalog,client,"http://127.0.0.1:18083/search","hybrid").retrieve("회사에 낼 증빙",40);
        assertEquals("hybrid",result.mode());assertEquals(40,result.menus().size());
        assertEquals(40,new HashSet<>(result.menus()).size());assertEquals(40,catalog.selectionFacts(result.menus()).size());server.verify();
    }
    @Test void rejectsStaleIndexAndFallsBackToLexical() throws Exception {
        var catalog=new MenuCatalog(mapper);var client=new RestTemplate();var server=MockRestServiceServer.createServer(client);
        server.expect(requestTo("http://127.0.0.1:18083/search"))
                .andRespond(withSuccess("{\"catalog_sha256\":\"stale\",\"ids\":[\"kb_menu_0001\"]}",MediaType.APPLICATION_JSON));
        var result=new HybridMenuRetriever(catalog,client,"http://127.0.0.1:18083/search","hybrid").retrieve("조회",40);
        assertEquals("lexical_fallback",result.mode());assertEquals(catalog.candidates("조회",40),result.menus());server.verify();
    }
    @Test void semanticEndpointCannotSendRequestsToExternalHost() throws Exception {
        var catalog=new MenuCatalog(mapper);
        assertThrows(IllegalArgumentException.class,()->new HybridMenuRetriever(catalog,new RestTemplate(),"https://example.com/search","hybrid"));
    }
}
