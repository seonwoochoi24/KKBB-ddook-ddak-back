package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import java.nio.file.*;
import java.util.*;

/** Local benchmark utility; no JEV calls and no production HTTP endpoint. */
public class MenuRetrievalAudit {
    public static void main(String[] args) throws Exception {
        var mapper=new ObjectMapper();var catalog=new MenuCatalog(mapper);
        var hybrid=new HybridMenuRetriever(catalog,new RestTemplate(),"http://127.0.0.1:18083/search","hybrid");
        var rows=new ArrayList<Map<String,Object>>();
        for (var row:mapper.readTree(Files.readAllBytes(Path.of(args[0]))).path("cases")) {
            if (row.path("expected_menu_ids").isEmpty()) continue;
            String message=row.path("message").asText(),expected=row.path("expected_menu_ids").get(0).asText();
            var lex=catalog.candidates(message,40).stream().map(MenuCatalog.Menu::id).toList();
            var result=hybrid.retrieve(message,40);
            if (!result.mode().equals("hybrid")) throw new IllegalStateException("Audit fallback");
            var semantic=result.menus().stream().map(MenuCatalog.Menu::id).toList();
            rows.add(Map.of("id",row.path("id").asText(),"expected",expected,"lexical_rank",lex.indexOf(expected)+1,"hybrid_rank",semantic.indexOf(expected)+1));
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of(args[1]).toFile(),rows);
    }
}
