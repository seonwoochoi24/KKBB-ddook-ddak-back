<div align="center">

# 깨비뚝딱 Backend ✨

### 자연어 금융 요청을 검증 가능한 위젯 JSON으로 변환하는 Generative UI API

[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Grand Prize](https://img.shields.io/badge/GRAND_PRIZE-금융감독원상-FFCC00?style=for-the-badge&labelColor=2D2D2D)](https://www.hankyung.com/article/202609032008P)
[![Live Demo](https://img.shields.io/badge/LIVE_DEMO-222222?style=for-the-badge&logo=netlify&logoColor=00C7B7)](https://kkbb-ddookddak.netlify.app)

</div>

## 🏆 제8회 Future Finance AI Challenge 대상

> **풀스택 선우**팀의 **깨비뚝딱**이 대상인 **금융감독원상**을 수상했습니다.

- 본선: 2026년 9월 2일, 이화여자대학교 이삼봉홀
- 경쟁: 전국 약 300개 참가팀 중 본선 10팀 진출
- 팀: 장선우(기획·디자인), 최선우(기획·프론트엔드), 박선우(기획·백엔드)
- 수상 기사: [한국경제](https://www.hankyung.com/article/202609032008P) · [더퍼스트미디어](https://www.thefirstmedia.net/news/articleView.html?idxno=207727) · [스마트투데이](https://www.smarttoday.co.kr/ko-kr/articles/111151)

<p align="center">
  <img src="./docs/images/kb-ai-challenge-fullstack-sunwoo.png" width="900" alt="풀스택 선우 팀 소개 - 장선우, 최선우, 박선우" />
</p>

## 개요

깨비뚝딱 백엔드는 사용자의 자연어 금융 요청을 분석해 React 프론트엔드가 렌더링할 수 있는 구조화된 위젯 JSON을 반환합니다.

```text
사용자 자연어
  → POST /api/canvas
  → jev 위젯 선택 (1회 호출)
  → 서버 검증·명확한 여행 파라미터 추출
  → 승인 위젯 선택 및 파라미터 추출
  → 서버 데이터 빌드·환율 조회
  → CanvasResponse JSON
```

AI는 필요한 금융 기능을 선택하고, 정확한 환율과 계산 값은 서버가 담당합니다. 새 흐름과 입력 제한은 [jev 화면 생성](docs/JEV_CANVAS.md)을 참고하세요. 생성 가능한 범위는 9개 위젯 enum으로 제한되어 있습니다.

## 주요 기능

- jev 기반 위젯 선택: `/api/canvas`에서 1회 호출, GPT 추가 호출 없음
- 최대 3개 금융 위젯 선택 및 조합
- 알려진 여행지·통화 및 명확한 외화 금액·여행 기간을 서버에서 추출 (제한된 규칙 기반)
- 깨비는 짧은 고정 문구만 반환하며 별도 설명 생성은 보류
- 불확실한 위젯 선택이나 충돌하는 파라미터는 확인 요청
- `open.er-api.com` 기반 실시간 환율 조회
- 환율 데이터 6시간 인메모리 캐시
- jev 장애는 명시적 오류 응답, 환율 API 장애는 기존 캐시·지원 불가 처리
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

`JevCanvasService`는 jev가 고른 위젯을 검증하고 `CanvasParameters`로 명확한 통화·금액·기간을 읽습니다. AI가 환율 숫자를 생성하지 않습니다. `ExchangeRateService`가 외부 API에서 실제 값을 조회하고 `WidgetDataService`가 최종 위젯 데이터를 계산합니다.

### 2. 승인된 위젯만 생성

고정된 9개 후보에 대한 jev 판정만 받고, 서버가 허용 목록·최대 3개·중복·검토 필요 여부를 검사합니다. 프론트엔드에서 구현되지 않은 임의의 컴포넌트나 코드를 실행하지 않습니다.

### 3. 장애 폴백

빈 메시지는 AI를 호출하지 않습니다. jev 호출 실패·잘못된 응답은 502/504, 키 미설정은 503으로 반환합니다. 불확실한 선택이나 모호한 파라미터는 `status=needs_clarification`과 빈 위젯으로 확인을 요청합니다. 환율 API 장애 시 기존 캐시를 유지하며, 캐시도 없으면 지원 불가 상태를 반환합니다.

## 실행 방법

### 요구사항

- Java 17+
- JEV API Key (`JEV_AI_API_KEY`)

### macOS / Linux

```bash
export JEV_AI_API_KEY="your-jev-api-key"
./mvnw spring-boot:run
```

### Windows PowerShell

```powershell
$env:JEV_AI_API_KEY="your-jev-api-key"
./mvnw.cmd spring-boot:run
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

## 환경 변수

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `JEV_AI_API_KEY` | Yes | 기본 화면 생성용 jev 인증 키 |
| `JEV_MODEL` | No | 기본 `jev-latest` |
| `JEV_MIN_CONFIDENCE` | No | 기본 0.6, 확률 보정이 검증된 값은 아님 |
| `OPENAI_API_KEY` | No | 기존 GPT 비교 실험에만 필요 |


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
