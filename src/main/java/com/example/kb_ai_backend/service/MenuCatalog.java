package com.example.kb_ai_backend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** The JSON file is the only source of selectable menus. Loaded once at startup. */
@Component
public class MenuCatalog {
    public record Menu(String id, String name, String description,
                       @JsonProperty("select_when") List<String> selectWhen) {}
    private final Map<String, Menu> menus;
    private final String sha256;
    private final com.fasterxml.jackson.databind.JsonNode metadata;

    public MenuCatalog(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("kb_starbanking_menu_structure.json").getInputStream()) {
            byte[] bytes = input.readAllBytes();
            try { sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
            catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
            var root = mapper.readTree(bytes);
            if (!root.path("menus").isArray() || root.path("menus").isEmpty())
                throw new IllegalStateException("Menu catalog must contain menus");
            var entries = new LinkedHashMap<String, Menu>();
            var names = new HashSet<String>();
            for (var node : root.path("menus")) {
                var menu = mapper.treeToValue(node, Menu.class);
                if (menu.id() == null || !menu.id().matches("kb_menu_\\d{4,}")
                        || menu.name() == null || menu.name().isBlank()
                        || menu.description() == null || menu.description().isBlank()
                        || menu.selectWhen() == null || menu.selectWhen().isEmpty()
                        || menu.selectWhen().stream().anyMatch(s -> s == null || s.isBlank())
                        || !names.add(menu.name()) || entries.putIfAbsent(menu.id(), menu) != null)
                    throw new IllegalStateException("Invalid or duplicate menu catalog entry");
                entries.put(menu.id(), new Menu(menu.id(), menu.name(), menu.description(), List.copyOf(menu.selectWhen())));
            }
            if (!root.path("menu_count").isIntegralNumber() || root.path("menu_count").intValue() != entries.size())
                throw new IllegalStateException("Menu count does not match catalog");
            menus = Collections.unmodifiableMap(entries);
            try (var facts = new ClassPathResource("menu_selection_metadata.json").getInputStream()) {
                metadata = mapper.readTree(facts);
                if (!metadata.isObject()) throw new IllegalStateException("Invalid selection metadata");
                var keys = new HashSet<String>();metadata.fieldNames().forEachRemaining(keys::add);
                if (!keys.equals(menus.keySet())) throw new IllegalStateException("Metadata must match menu IDs");
            }
        }
    }
    /** Weighted character-bigram retrieval handles Korean particles without a tokenizer or DB. */
    @Deprecated // Retained for historical benchmarks; the canvas uses semantic-only retrieval.
    public List<Menu> candidates(String request, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Candidate limit must be positive");
        var query = grams(request);
        var documents = new LinkedHashMap<Menu, Set<String>>();
        var frequency = new HashMap<String, Integer>();
        for (var menu : menus.values()) {
            var terms = grams(menu.name() + " " + menu.description() + " " + String.join(" ", menu.selectWhen()));
            documents.put(menu, terms);
            for (var term : terms) frequency.merge(term, 1, Integer::sum);
        }
        var scores = new HashMap<String, Double>();
        for (var menu : menus.values()) {
            var name = grams(menu.name());
            double score = 0;
            for (var term : query) {
                if (documents.get(menu).contains(term)) {
                    double idf = Math.log(1 + (double) menus.size() / frequency.get(term));
                    score += idf * (name.contains(term) ? 3 : 1);
                }
            }
            scores.put(menu.id(), score);
        }
        return menus.values().stream().sorted(Comparator
                .comparingDouble((Menu m) -> scores.get(m.id())).reversed().thenComparing(Menu::id))
                .limit(limit).toList();
    }
    private static Set<String> grams(String text) {
        var result = new HashSet<String>();
        for (var word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.length() == 1) result.add(word);
            for (int i = 0; i + 1 < word.length(); i++) result.add(word.substring(i, i + 2));
        }
        return result;
    }
    public String sha256() { return sha256; }
    public Map<String,Object> selectionFacts(List<Menu> candidates) {
        var facts = new LinkedHashMap<String,Object>();
        for (var menu : candidates) facts.put(menu.id(), metadata.path(menu.id()));
        return facts;
    }
    public Collection<Menu> all() { return menus.values(); }
    public Menu find(String id) { return menus.get(id); }
}
