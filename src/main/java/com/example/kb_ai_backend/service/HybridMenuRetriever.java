package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.util.*;

/** Fuse lexical and local multilingual E5 rankings; never query an external embedding API. */
@Deprecated
public class HybridMenuRetriever {
    private static final Logger log = LoggerFactory.getLogger(HybridMenuRetriever.class);
    private final MenuCatalog catalog;
    private final RestTemplate client;
    private final String url;
    private final boolean hybrid;
    @Autowired
    public HybridMenuRetriever(MenuCatalog catalog,
            @Value("${MENU_SEMANTIC_URL:http://127.0.0.1:18083/search}") String url,
            @Value("${MENU_RETRIEVAL_MODE:hybrid}") String mode) {
        this(catalog, newClient(), url, mode);
    }
    HybridMenuRetriever(MenuCatalog catalog, RestTemplate client, String url, String mode) {
        if (!Set.of("hybrid", "lexical").contains(mode)) throw new IllegalArgumentException("Invalid retrieval mode");
        var uri = URI.create(url);
        if (!"http".equals(uri.getScheme()) || !Set.of("127.0.0.1", "localhost", "::1").contains(uri.getHost())
                || uri.getUserInfo() != null || uri.getQuery() != null)
            throw new IllegalArgumentException("Semantic endpoint must be local HTTP");
        this.catalog=catalog; this.client=client; this.url=url; this.hybrid=mode.equals("hybrid");
    }
    private static RestTemplate newClient() {
        var factory=new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(300); factory.setReadTimeout(1500);
        return new RestTemplate(factory);
    }
    public record Retrieved(List<MenuCatalog.Menu> menus, String mode) {}
    public Retrieved retrieve(String message, int limit) {
        var lexical=catalog.candidates(message, Math.max(limit,80));
        if (!hybrid) return new Retrieved(lexical.stream().limit(limit).toList(), "lexical");
        try {
            var bytes=new ObjectMapper().writeValueAsBytes(Map.of("message",message,"limit",80,"catalog_sha256",catalog.sha256()));
            var headers=new HttpHeaders();headers.setContentType(MediaType.APPLICATION_JSON);
            var response=client.postForObject(url,new HttpEntity<>(bytes,headers),JsonNode.class);
            if (response==null || !catalog.sha256().equals(response.path("catalog_sha256").asText()) || !response.path("ids").isArray()
                    || response.path("ids").isEmpty() || response.path("ids").size()>100) throw new IllegalStateException("Invalid semantic ranking");
            var semantic=new ArrayList<String>();
            for (var id : response.path("ids")) {
                if (!id.isTextual() || catalog.find(id.asText())==null || semantic.contains(id.asText())) throw new IllegalStateException("Invalid semantic ID");
                semantic.add(id.asText());
            }
            var scores=new HashMap<String,Double>();
            for (int i=0;i<lexical.size();i++) scores.merge(lexical.get(i).id(),1.0/(60+i+1),Double::sum);
            for (int i=0;i<semantic.size();i++) scores.merge(semantic.get(i),1.0/(60+i+1),Double::sum);
            var fused=scores.entrySet().stream().sorted(Map.Entry.<String,Double>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                    .limit(limit).map(e->catalog.find(e.getKey())).toList();
            return new Retrieved(fused,"hybrid");
        } catch (Exception ex) {
            log.warn("Local semantic search unavailable; using lexical candidates ({})",ex.getClass().getSimpleName());
            return new Retrieved(lexical.stream().limit(limit).toList(),"lexical_fallback");
        }
    }
}
