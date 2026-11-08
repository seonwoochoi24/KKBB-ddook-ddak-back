package com.example.kb_ai_backend.service;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.client.MockRestServiceServer;
import java.util.Map;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;
class JevDecisionClientTest {
    Map<String,JevDecisionClient.Question> questions(){return Map.of("need_1",new JevDecisionClient.Question("choose",Map.of("known","known","none","none")));}
    @Test void rejectsUnknownChoiceEvenWhenConfidenceIsHigh() {
        var client=new RestTemplate();var server=MockRestServiceServer.createServer(client);
        server.expect(requestTo("https://api.typesafe.ai/v1/systemone")).andRespond(withSuccess("{\"answers\":{\"need_1\":{\"type\":\"choice\",\"choice\":\"invented\",\"confidence\":0.99}}}",MediaType.APPLICATION_JSON));
        var service=new JevDecisionClient(client,"test-key","test-model");
        assertEquals(502,assertThrows(ResponseStatusException.class,()->service.decide(Map.of(),questions())).getStatusCode().value());server.verify();
    }
    @Test void missingKeyMakesNoRequest() {
        var client=new RestTemplate();var server=MockRestServiceServer.createServer(client);
        assertEquals(503,assertThrows(ResponseStatusException.class,()->new JevDecisionClient(client,"","test").decide(Map.of(),questions())).getStatusCode().value());server.verify();
    }
}
