package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.*;

/** Semantic-only retrieval. No keyword search and no lexical fallback. */
@Service
public class SemanticMenuRetriever {
    private final MenuCatalog catalog;
    private final RestTemplate client;
    private final String url;
    @Autowired
    public SemanticMenuRetriever(MenuCatalog catalog,@Value("${MENU_SEMANTIC_URL:http://127.0.0.1:18083/search}") String url) {
        this(catalog,newClient(),url);
    }
    SemanticMenuRetriever(MenuCatalog catalog,RestTemplate client,String url) {
        var uri=URI.create(url);
        if(!"http".equals(uri.getScheme()) || !Set.of("127.0.0.1","localhost","::1").contains(uri.getHost()) || uri.getUserInfo()!=null || uri.getQuery()!=null)
            throw new IllegalArgumentException("Semantic endpoint must be local HTTP");
        this.catalog=catalog;this.client=client;this.url=url;
    }
    private static RestTemplate newClient(){var f=new SimpleClientHttpRequestFactory();f.setConnectTimeout(300);f.setReadTimeout(2000);return new RestTemplate(f);}
    public List<MenuCatalog.Menu> retrieve(String query,int limit) {
        if(limit<1 || limit>40)throw new IllegalArgumentException("Invalid semantic candidate limit");
        try {
            var bytes=new ObjectMapper().writeValueAsBytes(Map.of("message",query,"limit",limit,"catalog_sha256",catalog.sha256()));
            var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);
            var result=client.postForObject(url,new HttpEntity<>(bytes,headers),JsonNode.class);
            if(result==null || !catalog.sha256().equals(result.path("catalog_sha256").asText()) || !result.path("ids").isArray() || result.path("ids").isEmpty() || result.path("ids").size()>limit)throw new IllegalStateException();
            var menus=new ArrayList<MenuCatalog.Menu>();var seen=new HashSet<String>();
            for(var id:result.path("ids")) {
                var menu=id.isTextual()?catalog.find(id.asText()):null;
                if(menu==null || !seen.add(menu.id()))throw new IllegalStateException();
                menus.add(menu);
            }
            return List.copyOf(menus);
        } catch(Exception ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"로컬 의미검색을 사용할 수 없습니다. 검색 서버를 확인해주세요.");}
    }
}
