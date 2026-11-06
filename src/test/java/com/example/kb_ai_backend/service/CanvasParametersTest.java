package com.example.kb_ai_backend.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CanvasParametersTest {
    CanvasParameters parser = new CanvasParameters();
    @Test void extractsCommonCityAmountAndDays() {
        var values = parser.extract("상하이 3박4일 여행 2,000 CNY 환전");
        assertEquals("상하이", values.destination()); assertEquals("CNY", values.currency());
        assertEquals(2000, values.amount()); assertEquals(4, values.tripDays()); assertFalse(values.needsReview());
    }
    @Test void handlesKoreanMoneyUnitsAndExplicitCurrencyOverride() {
        var values = parser.extract("일본 여행 2만 달러 환전 일주일");
        assertEquals("USD", values.currency()); assertEquals(20000, values.amount());
        assertEquals(7, values.tripDays()); assertFalse(values.needsReview());
        assertEquals("HKD", parser.extract("홍콩 100홍콩달러").currency());
        assertEquals(100, parser.extract("USD 100 환전").amount());
    }
    @Test void absentValuesRemainAbsent() {
        var values = parser.extract("여행 준비 도와줘");
        assertNull(values.currency()); assertNull(values.amount()); assertNull(values.destination()); assertNull(values.tripDays());
    }
    @Test void rejectsConflictingUnsafeAndUnrepresentableValues() {
        for (String message : new String[]{"일본 중국 여행", "100 USD 100 JPY", "일본 -100엔", "일본 0엔",
                "일본 999999999999999엔", "일본 10.5엔", "일본 3박8일", "일본 0일", "일본 999999999999일",
                "일본 1만5천엔", "일본 오천엔", "일본 1억엔", "일본 100만원어치 엔화"})
            assertTrue(parser.extract(message).needsReview(), message);
    }
}
