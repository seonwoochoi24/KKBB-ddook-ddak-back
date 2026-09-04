<div align="center">

# 깨비뚝딱 Backend ✨

### 자연어 금융 요청을 검증 가능한 위젯 JSON으로 변환하는 Generative UI API

[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Live Demo](https://img.shields.io/badge/LIVE_DEMO-222222?style=for-the-badge&logo=netlify&logoColor=00C7B7)](https://kkbb-ddookddak.netlify.app)

</div>

## 개요

깨비뚝딱 백엔드는 사용자의 자연어 금융 요청을 분석해 React 프론트엔드가 렌더링할 수 있는 구조화된 위젯 JSON을 반환합니다.

```text
사용자 자연어
  → POST /api/canvas
  → GPT-4o-mini Function Calling
  → 승인 위젯 선택 및 파라미터 추출
  → 서버 데이터 빌드·환율 조회
  → CanvasResponse JSON
```

AI는 필요한 금융 기능을 선택하고, 정확한 환율과 계산 값은 서버가 담당합니다. 생성 가능한 범위는 9개 위젯 enum으로 제한되어 있습니다.

## 주요 기능

- `gpt-4o-mini` Function Calling 기반 복합 의도 분석
- 최대 3개 금융 위젯 선택 및 조합
- 여행지, ISO 4217 통화, 요청 금액, 여행 기간, 모임명 추출
- `open.er-api.com` 기반 실시간 환율 조회
- 환율 데이터 6시간 인메모리 캐시
- OpenAI 또는 환율 API 장애 시 안전한 폴백
- 로컬 Vite 및 Netlify 배포 주소 CORS 허용

## API

### `POST /api/canvas`

요청:

```json
{
  "message": "상하이로 3박 4일 여행 가는데 환전과 QR 결제, 보험을 준비해줘"
}
```

응답:

```json
{
  "greeting": "상하이 여행이라니, 깨비와 함께 즐거운 여행을 준비해봐요!",
  "widgets": [
    {
      "type": "travel_card_charge_widget",
      "currency": "CNY",
      "supported": true,
      "exchangeRate": 190.0
    },
    {
      "type": "overseas_qr_payment_widget"
    },
    {
      "type": "travel_insurance_widget"
    }
  ]
}
```

> `exchangeRate` 값은 예시입니다. 실제 응답에서는 외부 환율 API 조회 결과가 사용됩니다.

## 지원 시나리오와 위젯

| 시나리오 | 위젯 |
| --- | --- |
| 해외여행 준비 | `travel_card_charge_widget`, `overseas_qr_payment_widget`, `travel_insurance_widget` |
| 동아리 회비 관리 | `group_account_status`, `expense_report_widget`, `group_card_safety_widget` |
| 지갑 분실 긴급 대응 | `card_freeze_all`, `atm_smart_withdrawal`, `id_reissue_status_widget` |

## 핵심 설계

### 1. LLM과 금융 계산 분리

`OpenAiApiService`는 사용자의 의도와 통화 코드를 추론하지만 환율 숫자를 생성하지 않습니다. `ExchangeRateService`가 외부 API에서 실제 값을 조회하고 `WidgetDataService`가 최종 위젯 데이터를 계산합니다.

### 2. 승인된 위젯만 생성

Function Calling의 `enum`으로 허용 위젯을 제한합니다. 프론트엔드에서 구현되지 않은 임의의 컴포넌트나 코드를 실행하지 않습니다.

### 3. 장애 폴백

빈 메시지는 LLM을 호출하지 않습니다. OpenAI 네트워크 오류, 인증 오류, 사용량 제한 또는 응답 파싱 실패가 발생하면 빈 위젯 목록을 반환합니다. 환율 API 장애 시 기존 캐시를 유지하며, 캐시도 없으면 지원 불가 상태를 반환합니다.

## 실행 방법

### 요구사항

- Java 17+
- OpenAI API Key

### macOS / Linux

```bash
export OPENAI_API_KEY="your-openai-api-key"
./mvnw spring-boot:run
```

### Windows PowerShell

```powershell
$env:OPENAI_API_KEY="your-openai-api-key"
./mvnw.cmd spring-boot:run
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 환경 변수

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `OPENAI_API_KEY` | Yes | OpenAI Chat Completions API 인증 키 |


## 테스트

```bash
./mvnw test
```

API 직접 호출:

```bash
curl -X POST http://localhost:8080/api/canvas \
  -H "Content-Type: application/json" \
  -d '{"message":"지갑을 잃어버렸어. 긴급 조치 화면을 만들어줘"}'
```

## 프로젝트 구조

```text
src/main/java/com/example/kb_ai_backend
├── config
│   └── CorsConfig.java
├── controller
│   └── CanvasController.java
└── service
    ├── OpenAiApiService.java
    ├── WidgetDataService.java
    └── ExchangeRateService.java
```

## 향후 확장

- KB 계좌·카드 API 어댑터
- OAuth 및 본인인증
- 정부24·경찰청 공공 API
- 결제·ATM·보험 API
- 감사 로그, 개인정보 암호화, 운영 모니터링

> 핵심 생성 기술은 이미 구현되어 있으며, 이후 확장은 인증·보안·외부 금융 API를 단계적으로 연결하는 엔지니어링 과제입니다.
