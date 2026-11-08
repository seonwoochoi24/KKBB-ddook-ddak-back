package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Validated typed decisions; never accepts a choice outside the supplied options. */
@Service
public class JevDecisionClient {
    private final RestTemplate client;
    private final String key, model;
    @Autowired
    public JevDecisionClient(@Value("${JEV_AI_API_KEY:}") String key,@Value("${JEV_MODEL:jev-latest}") String model) {
        this(newClient(),key,model);
    }
    JevDecisionClient(RestTemplate client,String key,String model) {this.client=client;this.key=key;this.model=model;}
    private static RestTemplate newClient() {
        var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(3000);f.setReadTimeout(30000);return new RestTemplate(f);
    }
    public record Question(String instructions, Map<String,String> criteria) {}
    public record Choice(String choice,double confidence) {}
    public record Decisions(Map<String,Choice> answers,long durationMs) {}
    public Decisions decide(Map<String,Object> state,Map<String,Question> definitions) {
        if (key==null || key.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"JEV_AI_API_KEY가 실행 환경에 없습니다.");
        var questions=new LinkedHashMap<String,Object>();
        definitions.forEach((id,q)-> {
            if(q.criteria().size()<2 || q.criteria().size()>255) throw new IllegalArgumentException("Invalid choice count");
            questions.put(id,Map.of("type","choice","instructions",q.instructions(),"criteria",q.criteria()));
        });
        var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);headers.setBearerAuth(key);
        long start=System.nanoTime();
        try {
            var response=client.postForObject("https://api.typesafe.ai/v1/systemone",new HttpEntity<>(Map.of("model",model,"state",state,"questions",questions),headers),JsonNode.class);
            if(response==null)throw invalid();
            var results=new LinkedHashMap<String,Choice>();
            for(var entry:definitions.entrySet()) {
                var a=response.path("answers").path(entry.getKey());
                if(!a.path("choice").isTextual() || !a.path("confidence").isNumber() || !"choice".equals(a.path("type").asText()))throw invalid();
                String choice=a.path("choice").asText();double confidence=a.path("confidence").doubleValue();
                if(!entry.getValue().criteria().containsKey(choice) || !Double.isFinite(confidence) || confidence<0 || confidence>1)throw invalid();
                results.put(entry.getKey(),new Choice(choice,confidence));
            }
            return new Decisions(Collections.unmodifiableMap(results),TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
        } catch(HttpStatusCodeException ex){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"jev API 오류: HTTP "+ex.getStatusCode().value());}
        catch(ResourceAccessException ex){throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,"jev API 연결 실패 또는 시간 초과");}
        catch(ResponseStatusException ex){throw ex;}
        catch(Exception ex){throw invalid();}
    }
    private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"상황 판단 API 응답 형식이 올바르지 않습니다.");}
}
