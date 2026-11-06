package com.example.kb_ai_backend.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.concurrent.TimeUnit;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * open.er-api.com (무료, API 키 불필요)에서 실시간 환율을 받아와 캐싱하는 서비스.
 * KRW 기준으로 조회해서 "1 외화 = 몇 원"을 계산해준다.
 */
@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private static final String API_URL = "https://open.er-api.com/v6/latest/KRW";
    private static final long CACHE_TTL_MILLIS = 1000 * 60 * 60 * 6; // 6시간 캐싱

    private final RestTemplate restTemplate = new RestTemplate();

    private volatile Map<String, Double> cachedRates = null; // KRW -> 외화 환율 (1원 = ? 외화)
    private volatile long lastFetchedAt = 0;

    /**
     * 지정한 통화 코드(ISO 4217, 예: "USD", "MNT")의 "1단위당 원화 환율"을 반환.
     * 지원하지 않는 통화거나 API 호출 실패 시 null 반환 (절대 임의의 값을 만들어내지 않음).
     */
    @SuppressWarnings("unchecked")
    public synchronized Double getKrwRateFor(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return null;
        }

        String code = currencyCode.toUpperCase();

        refreshCacheIfStale();

        if (cachedRates == null) {
            return null; // API 호출 자체가 실패한 경우
        }

        Double krwToForeign = cachedRates.get(code); // 1원 = krwToForeign 외화
        if (krwToForeign == null || krwToForeign == 0) {
            return null; // 이 API가 지원하지 않는 통화
        }

        return 1.0 / krwToForeign; // 1 외화 = 몇 원
    }

    @SuppressWarnings("unchecked")
    private void refreshCacheIfStale() {
        long now = System.currentTimeMillis();
        if (cachedRates != null && (now - lastFetchedAt) < CACHE_TTL_MILLIS) {
            log.info("performance requestId={} stage=exchange_cache cache=hit", MDC.get("requestId"));
            return; // 캐시 아직 유효함
        }

        long started = System.nanoTime();
        boolean refreshed = false;
        try {
            Map<String, Object> response = restTemplate.getForObject(API_URL, Map.class);
            if (response == null) return;

            Object ratesObj = response.get("rates");
            if (!(ratesObj instanceof Map)) return;

            Map<String, Object> rawRates = (Map<String, Object>) ratesObj;
            Map<String, Double> parsed = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : rawRates.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    parsed.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }
            }

            cachedRates = parsed;
            lastFetchedAt = now;
            refreshed = true;

        } catch (Exception e) {
            e.printStackTrace();
            // 실패해도 기존 캐시(있다면)는 유지, 없으면 null 그대로 둠
        } finally {
            log.info("performance requestId={} stage=exchange_refresh durationMs={} refreshed={}",
                    MDC.get("requestId"),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), refreshed);
        }
    }
}