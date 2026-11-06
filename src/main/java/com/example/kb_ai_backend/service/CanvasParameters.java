package com.example.kb_ai_backend.service;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/** Conservative single-turn extraction. Unknown values remain absent; no LLM call. */
@Component
public class CanvasParameters {
    private static final Map<String, String> DESTINATIONS = Map.ofEntries(
            Map.entry("상하이", "CNY"), Map.entry("베이징", "CNY"), Map.entry("중국", "CNY"),
            Map.entry("일본", "JPY"), Map.entry("도쿄", "JPY"), Map.entry("오사카", "JPY"),
            Map.entry("미국", "USD"), Map.entry("뉴욕", "USD"), Map.entry("하와이", "USD"),
            Map.entry("태국", "THB"), Map.entry("방콕", "THB"), Map.entry("베트남", "VND"),
            Map.entry("다낭", "VND"), Map.entry("홍콩", "HKD"), Map.entry("싱가포르", "SGD"),
            Map.entry("영국", "GBP"), Map.entry("런던", "GBP"), Map.entry("프랑스", "EUR"),
            Map.entry("파리", "EUR"), Map.entry("독일", "EUR"), Map.entry("몽골", "MNT"),
            Map.entry("부탄", "BTN"));
    private static final Map<String, String> UNITS = Map.ofEntries(
            Map.entry("위안", "CNY"), Map.entry("위안화", "CNY"), Map.entry("엔", "JPY"),
            Map.entry("엔화", "JPY"), Map.entry("달러", "USD"), Map.entry("미국달러", "USD"),
            Map.entry("홍콩달러", "HKD"), Map.entry("싱가포르달러", "SGD"),
            Map.entry("유로", "EUR"), Map.entry("파운드", "GBP"), Map.entry("바트", "THB"),
            Map.entry("동", "VND"), Map.entry("투그릭", "MNT"), Map.entry("눌트럼", "BTN"),
            Map.entry("원", "KRW"), Map.entry("원화", "KRW"));
    private static final String UNIT = "홍콩달러|싱가포르달러|미국달러|위안화|엔화|원화|위안|달러|유로|파운드|바트|투그릭|눌트럼|엔|동|원|(?i:[A-Z]{3})(?![A-Za-z])";
    private static final Pattern AMOUNT = Pattern.compile("(?<![\\d.,])(-?\\d[\\d,]*(?:\\.\\d+)?)\\s*([만천]?)\\s*(" + UNIT + ")");
    private static final Pattern ISO = Pattern.compile("(?i)(?<![A-Za-z])[A-Z]{3}(?![A-Za-z])");
    private static final Pattern NAMED_CURRENCY = Pattern.compile("홍콩달러|싱가포르달러|미국달러|위안화|엔화|위안|달러|유로|파운드|바트|투그릭|눌트럼");
    private static final Pattern DAYS = Pattern.compile("(?<!\\d)(\\d+)\\s*(?:박\\s*(\\d+)\\s*일|일(?:간)?|박(?!\\s*\\d))");
    private static final Pattern COMPLEX_AMOUNT = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?\\s*[만천]\\s*\\d|(?<![\\d일이삼사오육칠팔구십백천만억])[일이삼사오육칠팔구십백천만억]+\\s*(?:" + UNIT + ")|\\d\\s*억");
    private static final Pattern PREFIX_AMOUNT = Pattern.compile("(?i)(?<![A-Za-z])([A-Z]{3})\\s+(-?\\d[\\d,]*(?:\\.\\d+)?\\s*[만천]?)(?![\\d.,])");

    public record Values(String currency, String destination, Integer amount, Integer tripDays, boolean needsReview) {}

    public Values extract(String message) {
        var places = DESTINATIONS.keySet().stream().filter(message::contains)
                .sorted(Comparator.<String>comparingInt(message::indexOf).thenComparing(Comparator.comparingInt(String::length).reversed())).toList();
        var inferred = new HashSet<String>();
        places.forEach(place -> inferred.add(DESTINATIONS.get(place)));
        var currencies = new HashSet<String>();
        var codes = ISO.matcher(message);
        boolean review = COMPLEX_AMOUNT.matcher(message).find();
        while (codes.find()) {
            String code = codes.group().toUpperCase(Locale.ROOT);
            // Only ISO codes understood by the JDK; arbitrary three-letter words are ignored.
            try { Currency.getInstance(code); currencies.add(code); } catch (IllegalArgumentException ignored) { }
        }
        var names = NAMED_CURRENCY.matcher(message);
        while (names.find()) currencies.add(UNITS.get(names.group()));
        Integer amount = null;
        var amounts = AMOUNT.matcher(PREFIX_AMOUNT.matcher(message).replaceAll("$2 $1"));
        int amountCount = 0;
        while (amounts.find()) {
            amountCount++;
            String unit = amounts.group(3);
            String code = UNITS.getOrDefault(unit, unit.toUpperCase(Locale.ROOT));
            try { Currency.getInstance(code); currencies.add(code); }
            catch (IllegalArgumentException invalid) { review = true; }
            try {
                BigDecimal number = new BigDecimal(amounts.group(1).replace(",", ""));
                int multiplier = switch (amounts.group(2)) { case "만" -> 10000; case "천" -> 1000; default -> 1; };
                amount = number.multiply(BigDecimal.valueOf(multiplier)).intValueExact();
                if (amount <= 0) review = true;
            } catch (ArithmeticException | NumberFormatException invalid) { review = true; }
        }
        if (amountCount > 1 || currencies.size() > 1 || inferred.size() > 1) review = true;
        String currency = currencies.size() == 1 ? currencies.iterator().next()
                : currencies.isEmpty() && inferred.size() == 1 ? inferred.iterator().next() : null;
        Integer days = null;
        var periods = DAYS.matcher(message);
        int periodCount = 0;
        while (periods.find()) {
            periodCount++;
            try {
                int first = Integer.parseInt(periods.group(1));
                days = periods.group(2) != null ? Integer.valueOf(periods.group(2))
                        : periods.group().endsWith("박") ? Math.addExact(first, 1) : first;
                if (days < 1 || days > 365 || (periods.group(2) != null && days != Math.addExact(first, 1))) review = true;
            } catch (ArithmeticException | NumberFormatException invalid) { review = true; }
        }
        if (periodCount > 1) review = true;
        if (days == null && message.contains("일주일")) days = 7;
        if (days == null && message.contains("당일치기")) days = 1;
        return new Values(currency, places.isEmpty() ? null : places.get(0), amount, days, review);
    }
}
