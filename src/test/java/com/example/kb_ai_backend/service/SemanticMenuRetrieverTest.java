package com.example.kb_ai_backend.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;
class SemanticMenuRetrieverTest {
    @Test void unavailableLocalModelReturns503WithoutLexicalFallback() throws Exception {
        var catalog=new MenuCatalog(new ObjectMapper());var client=new RestTemplate();var server=MockRestServiceServer.createServer(client);
        server.expect(requestTo("http://127.0.0.1:18083/search")).andRespond(withServerError());
        var retriever=new SemanticMenuRetriever(catalog,client,"http://127.0.0.1:18083/search");
        assertEquals(503,assertThrows(ResponseStatusException.class,()->retriever.retrieve("여행",12)).getStatusCode().value());server.verify();
    }
}
