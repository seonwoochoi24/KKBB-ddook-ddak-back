# 깨비뚝딱 작업 인수인계

기준일: 2026-11-06. 커밋 전 기준 HEAD `89c9722`와 작업 폴더를 비교해 **이번 커밋에 포함하는 변경 전체**를 정리했다. 최근 jev 연결뿐 아니라 그 전에 수정한 프롬프트·계측·비교 실험도 포함한다. 이후에는 현재 상태·다음 작업을 갱신하고 최신 작업 기록을 위에 추가한다.

## 현재 상태

| 항목 | 상태 |
|---|---|
| 협업 방향 | 원본은 기존 상태로 돌리고, 각자 포크에서 개발한 결과 중 KB 테크포럼 15분 발표에 넣을 개선만 선별하기로 합의 |
| 원본 복구 | 이 작업에서 실행·완료 확인하지 않음 |
| 작업 위치 | `/Users/seonwoo-choi/IdeaProjects/KKBB-ddook-ddak-back` |
| 저장소·브랜치 | origin: `https://github.com/seonwoochoi24/KKBB-ddook-ddak-back.git`, `main`, 변경 비교 기준 HEAD `89c9722` |
| 저장 상태 | 커밋 전 수정 6개·신규 미추적 14개, 총 20개 파일을 사용자 요청으로 이번 커밋에 포함. 원격 푸시·배포는 수행하지 않음 |
| 기본 화면 생성 | 로컬 구현: jev 선택 1회 → 서버 검증 → 규칙 기반 파라미터 추출 → 데이터 구성 → greeting/widgets/status 반환 |
| GPT 경로 | 프롬프트 수정·계측 코드를 보존. 기본 canvas에서는 호출하지 않음. 선택 전용 비교 API의 GPT 경로는 별도 구현 |
| 깨비 설명 | 상세 생성 보류. 성공 시 고정 인사말. 비동기 설명·스트리밍 미구현 |
| 발표 채택 | 이 포크의 후보 실험이며 팀 공통 구현·발표 채택은 아직 확정하지 않음 |
| 검증 | 코드 구현 당시 테스트 22개 통과. 현재 구현의 실제 jev·프론트·전체 속도는 미검증 |

## 다음 작업

| 순서 | 할 일 | 확인할 결과 |
|---|---|---|
| 1 | 실제 jev로 위젯 선택 재검증 | ‘만’ 없는 단일 기능, 복수 기능, 포괄적 준비, 제외 요청을 구분. 30개 평가 사례와 실패·검토 필요 여부 기록 |
| 2 | 기존 프론트 연동 확인 | 성공 위젯·확인 문구·미지원 요청·HTTP 오류 표시. 현재 파라미터 지원 범위가 시연에 충분한지 확인 |
| 3 | 전체 응답 시간 측정 | 선택·환율 캐시 적중/미적중·서버 전체·브라우저 표시 지연을 구분. jev 선택만의 시간과 혼용하지 않음 |
| 4 | 관찰된 실패부터 개선 | 선택 오류·정보 추출 누락을 재현 사례로 남겨 개선. 규칙 추출과 GPT 추출의 차이도 확인 |
| 5 | 발표용 개선 선별 | 각자 개발 결과를 비교해 개선점·측정 조건·시연·한계를 정리하고 발표에 넣을 항목만 선택. 원본 반영 범위는 별도 결정 |

## 작업 기록

### 2026-11-06 · 전체 변경 커밋

- 사용자 요청으로 아래 20개 변경 파일을 한 커밋에 포함한다.
- 커밋 직전 `./mvnw -o test` 재실행: 22개 통과. `git diff --check` 확인.
- 코드 추가 수정 없이 인수인계의 저장·검증 상태를 갱신했다. 원격 푸시·배포는 수행하지 않는다.

### 2026-11-06 · 전체 미커밋 변경 통합 인계

**한 일**

1. **GPT 프롬프트·출력 최적화**: HEAD의 ‘해외여행·환전이면 기본 3개, 특정 기능만 콕 집으면 1개’ 규칙을 바꾸어, **‘만’이 없어도 구체적 기능 요청을 우선**하도록 수정했다. 여행지만 언급했다고 보험·QR결제를 추가하지 않는다. 함수·필드 설명을 짧게 하고 temperature=0, null 필드 생략·간결한 JSON 출력·필요한 상황정보 추출 지침을 추가했다. 이는 앞선 미커밋 작업이며, 이번 jev 연결 시에는 수정된 정책을 재사용했다.
2. **지연·토큰 계측**: 요청 ID로 전체 요청, GPT HTTP, GPT 토큰, 환율 캐시·갱신, 공급자별 선택 지연을 연결해서 볼 수 있도록 기록을 추가했다. 새 성능 로그는 사용자 원문·키가 아니라 시간·상태·토큰 중심이다. 기존 오류 출력 전체에 대한 마스킹 작업을 완료한 것은 아니다.
3. **jev/GPT 비교 실험**: 두 공급자 모두 9개 후보에 대한 선택만 수행하는 API와 30개 합성 사례, 벤치마크 스크립트를 추가했다. jev는 choice·confidence를, GPT는 strict JSON Schema 판정을 반환한다. 실제 모델명·입출력 토큰·선택 시간·왕복 시간·요청 ID·정확 일치를 기록한다.
4. **기본 canvas를 jev로 연결**: 기존 연결 코드를 재사용해 jev를 1회 호출하고 서버에서 선택을 검증한다. 여행 파라미터는 제한된 규칙으로 읽고 깨비는 짧은 고정 인사말만 반환한다. 불확실한 선택·충돌 입력은 확인 요청, 공급자 실패는 HTTP 오류로 구분한다.
5. **실행·테스트·문서**: OpenAI 키를 선택 사항으로 바꾸고 Swagger UI 의존성, 최근 JDK용 Mockito 테스트 설정을 추가했다. 신규 테스트 21개와 기존 기동 테스트 1개를 확인했다. README·입력 제한 설명·인수인계 문서를 작성했다.
6. **협업 결정 반영**: 원본과 별도로 각자 개발하고 발표할 개선만 추리기로 한 현재 방향을 기록했다. 잘못 생성했던 AGENTS.md는 앞선 작업에서 handoff.md로 이전했다.

**건드린 파일/패키지 — 미커밋 전체 목록**

아래는 커밋 전 Git에서 확인한 모든 변경 파일이다. 아래 상태는 커밋 전 점검 당시 기준이다. 처음부터 수정/미추적 상태였던 파일도 제외하지 않았다. 실제 diff를 기준으로 정리했으며 변경자·세부 작업 시각은 Git만으로 모두 확정할 수 없다.

| 상태 | 파일 | 변경 내용 |
|---|---|---|
| 수정 | `README.md` | 기본 경로를 jev로 안내하고 처리 흐름·입력 제한·오류 처리·필수/선택 환경변수를 갱신. |
| 신규·미추적 | `docs/JEV_CANVAS.md` | jev 흐름·실행법·응답 상태·입력 지원 범위·검증 한계 문서 신규 작성. |
| 신규·미추적 | `handoff.md` | 모든 미커밋 변경의 현재 상태·결정·다음 작업·검증 및 파일 목록을 통합. |
| 수정 | `pom.xml` | Swagger UI용 springdoc 2.10.0 추가. 최근 JDK에서 Mockito 테스트가 동작하도록 Surefire에 명시적 Java agent와 Byte Buddy 설정 추가. |
| 신규·미추적 | `scripts/benchmark_widget_selection.py` | 공급자 순서를 번갈아 호출해 선택 정확 일치·검토 필요·지연·모델·토큰·요청 ID를 JSON 기록. 반복/사례 제한/공급자 선택, 평균·표본 P95 집계 지원. |
| 신규·미추적 | `src/main/java/com/example/kb_ai_backend/config/RequestTimingFilter.java` | 기본 canvas와 비교 API에 UUID requestId 생성, X-Request-Id 헤더·MDC 적용. 전체 서버 요청 시간·상태·완료 여부 기록 후 MDC 복원. |
| 수정 | `src/main/java/com/example/kb_ai_backend/controller/CanvasController.java` | GPT 직접 선택에서 JevCanvasService 호출로 전환. 기존 greeting/widgets 유지, status 추가 및 공급자 오류 응답 처리. |
| 신규·미추적 | `src/main/java/com/example/kb_ai_backend/controller/WidgetSelectionExperimentController.java` | POST /api/experiments/widget-selection?provider=jev 또는 openai 제공. 기본 jev, 공급자 실패는 명시적 HTTP 오류로 반환. |
| 신규·미추적 | `src/main/java/com/example/kb_ai_backend/service/CanvasParameters.java` | 알려진 여행지·통화, 명확한 금액·기간의 규칙 기반 추출. 명시 통화 우선, 복수/충돌/범위 초과/지원하지 않는 일부 금액 표현은 검토 요청. |
| 수정 | `src/main/java/com/example/kb_ai_backend/service/ExchangeRateService.java` | 캐시 적중과 갱신 시간·성공 여부를 requestId와 함께 기록. 기존 6시간 캐시와 환율 계산·장애 폴백은 유지. |
| 신규·미추적 | `src/main/java/com/example/kb_ai_backend/service/JevCanvasService.java` | jev 선택 1회, 허용 목록·중복·3개 제한·검토 여부 확인 후 서버 데이터 구성. GPT 추가 호출 없음. 빈 입력·미지원·확인 필요·정상 상태 구분, 깨비 고정 문구. |
| 수정 | `src/main/java/com/example/kb_ai_backend/service/OpenAiApiService.java` | 구체적 기능 우선으로 프롬프트 수정, 스키마 설명 축약, temperature=0, 불필요한 null/장문 출력 억제, 추출 규칙 명시. HTTP 시간·완료 여부·입출력 토큰 기록 추가. |
| 신규·미추적 | `src/main/java/com/example/kb_ai_backend/service/WidgetSelectionExperimentService.java` | 9개 위젯별 include/exclude/unsure 판정. jev와 GPT가 같은 선택 작업 수행. 신뢰도·응답 형식·모델·토큰 검증, 제한 시간·오류 상태 처리. 기본 canvas에서도 jev 경로를 재사용. |
| 수정 | `src/main/resources/application.properties` | OPENAI_API_KEY가 없어도 빈 기본값으로 기동 가능. 나머지 변경은 파일 끝 줄바꿈 정리. |
| 신규·미추적 | `src/test/java/com/example/kb_ai_backend/controller/CanvasControllerTest.java` | 1개: 기존 프론트 JSON 계약과 공급자 시간 초과 응답 검증. |
| 신규·미추적 | `src/test/java/com/example/kb_ai_backend/controller/WidgetSelectionExperimentControllerTest.java` | 1개: 비교 API의 공급자 오류가 빈 위젯 성공 응답으로 숨겨지지 않는지 검증. |
| 신규·미추적 | `src/test/java/com/example/kb_ai_backend/service/CanvasParametersTest.java` | 4개: 지명·통화·숫자 단위·기간 추출, 누락값, 충돌·잘못된 범위·지원 밖 표현 검증. |
| 신규·미추적 | `src/test/java/com/example/kb_ai_backend/service/JevCanvasServiceTest.java` | 8개: 선택 1회, 파라미터 전달, 빈/긴 입력, 불확실성, 허용 목록·중복·개수, 통화 누락·금액 충돌, 공급자 실패 검증. |
| 신규·미추적 | `src/test/java/com/example/kb_ai_backend/service/WidgetSelectionExperimentServiceTest.java` | 7개: 다중 선택·낮은 신뢰도·잘못된 응답·인증 오류·키 누락·입력·공급자 검증·동등 GPT 선택 작업을 모의 HTTP로 검증. |
| 신규·미추적 | `src/test/resources/widget-selection-cases.json` | 선택 평가용 합성 요청 30개와 기대 위젯 집합. 단일/복수 기능·포괄적 준비·국내 여행·부정·무관 요청·모임·분실·바꿔 말하기 포함. |

**다음 사람이 알아야 할 것**
- 선택 정책을 유지할 것: `나 상하이여행 환전하고 싶어` → 환전 하나, `환전하고 보험도 알아봐줘` → 환전·보험, 구체적 기능 없는 `일본 여행 준비 도와줘` → 여행 기본 조합. 제외 요청을 존중한다. 프롬프트에 규칙이 있다고 모델 정확도까지 보장되지는 않는다.
- 기본 canvas와 비교 API는 용도가 다르다. 비교 API는 위젯 선택만 하고 여행 정보·환율·렌더링을 포함하지 않는다. 그 속도로 전체 화면 성능을 설명하면 안 된다.
- 기본 경로는 jev만 호출한다. 기존 `OpenAiApiService`의 전체 선택·추출과 비교 API의 GPT 선택 전용 경로도 서로 다르다. 프롬프트 최적화 결과와 공급자 비교 결과를 같은 실험으로 합치지 않는다.
- 파라미터 규칙은 기존 GPT의 정보 추출 능력을 전부 대체하지 않는다. 알려진 지명·일부 금액·기간만 지원하며 모임명·대화 기억은 지원하지 않는다. 외화 금액이 없으면 기존 위젯의 기본 제안 금액이 사용된다. 상세 범위는 [jev 화면 생성](docs/JEV_CANVAS.md)을 참고한다.
- 필요한 키는 기본 경로의 `JEV_AI_API_KEY`, GPT 비교의 `OPENAI_API_KEY`다. jev 모델 기본값은 `jev-latest`, 신뢰도 임계값 기본값은 0.6이다. 임계값은 정확도 확률로 보정된 수치가 아니다.
- 금융 수치는 서버가 담당한다. 기존 금융 시연 데이터는 실제 계좌·거래 결과가 아니며 환율 조회는 동기 처리다. 깨비 인사말은 예전에도 템플릿이어서 ‘설명 생성 제거’ 자체로 빨라졌다고 주장하지 않는다.
- 각자 포크에서 개발 후 발표용 결과를 선별한다. 상대 저장소의 Qwen·논문 검색·MCP·지식 큐레이션을 이 포크의 필수 요구로 가져오지 않는다.

**막힌 것 / 넘기는 것**
- 실제 jev 호출·프론트 표시·전체 지연 재검증이 남아 있다. 이번 문서 정리에서 키 설정 여부는 확인하지 않았다.
- 여행 파라미터의 지원 밖 표현·누락을 먼저 평가해야 한다. 실제 금융기관 연결·본인인증·멀티턴·비동기 설명은 미구현이며 이번 범위 밖이다.
- Swagger 추가와 기존 GPT·환율 계측까지 아래 목록의 20개 파일을 모두 이번 커밋에 포함한다.
- 원본 복구 완료와 발표 채택 항목은 미확인·미정이다. 사용자 요청 없이 원격 푸시·공개 배포하지 않는다.

**프론트에 알려야 할 것**
- `POST /api/canvas`의 `greeting`, `widgets`는 유지. 문자열 `status`는 `ready`, `empty`, `unsupported`, `needs_clarification`이다.
- 확인 요청은 안내 인사말과 빈 위젯을 반환하므로 사용자에게 이유를 보여주는지 확인한다. 공급자 장애와 ‘선택된 기능 없음’을 구분한다.
- HTTP 400: 길이 초과, 502/504: 공급자 오류·응답 이상·시간 초과, 503: jev 키 미설정. 오류 본문은 숫자 `status`와 `error`이며 정상 응답과 형태가 다르다.
- `X-Request-Id`는 두 API의 응답에 추가된다. 프론트에서 직접 읽으려면 CORS 노출 설정도 별도 확인한다. 이번에 관련 프론트 코드를 수정하지 않았다.

**검증 결과**
- 코드 구현 당시 `./mvnw -o test`: 총 22개 통과, 실패·오류·건너뜀 0. 현재 남은 Surefire 보고서와도 일치한다. 구성은 선택 서비스 7개, 비교 컨트롤러 1개, 새 canvas 서비스 8개, 파라미터 4개, canvas 컨트롤러 1개, 기존 Spring 기동 1개다.
- 모의 HTTP/모의 서비스·규칙 검증과 기동 테스트이며 실제 모델의 의미 정확도나 지연 검증은 아니다. 최초 문서 정리에서는 테스트를 재실행하지 않았으나, 사용자 커밋 요청 후 `./mvnw -o test`를 재실행해 22개 통과를 확인했다. 실제 API 벤치마크는 재실행하지 않았다.
- 과거 2026-11-03 초기 실측: 6개 합성 사례에서 jev 평균 219ms, GPT 평균 1696.8ms, 두 공급자 모두 기대 위젯과 6/6 일치. `exchange` 사례는 ‘나 상하이여행 환전하고 싶어’였고 각각 245ms·1934ms로 환전 하나를 선택했다.
- 위 기록은 위젯 선택만의 과거 결과다. 지금 기본 canvas의 전체 지연, 일반 정확도, 운영 SLA로 확대하지 않는다. 작은 표본의 P95도 안정적인 성능 보장이 아니다.
- 과거 실측 원본은 저장소 밖 `/Users/seonwoo-choi/Documents/ChatGPT/브레인스토밍/깨비뚝딱_JEV_초기비교.json`에 있다. GPT 토큰 전후 비교 기록도 같은 폴더의 `깨비뚝딱_토큰비교_결과.json`에 있다. 이 두 파일은 현재 저장소의 미커밋 목록에 포함되지 않으며 이번에 복사하지 않았다.

**재개 명령**

```bash
# 변경 확인과 테스트 (키 없이 모의 검증 가능)
git status --short
./mvnw test
# 의존성이 준비된 환경에서는 ./mvnw -o test

# 실제 API 확인 전 환경에 JEV_AI_API_KEY 설정; GPT 비교에는 OPENAI_API_KEY도 필요
./mvnw spring-boot:run

# 별도 터미널: 선택 전용 비교. 실제 호출이 발생하므로 비용·호출량 확인
python3 scripts/benchmark_widget_selection.py --providers jev openai --limit 6 --repeats 1 --output /tmp/kkbb-widget-selection.json
```

**문서 변경**
- `README.md`: 기본 경로·실행 환경·오류 처리 안내.
- `docs/JEV_CANVAS.md`: API 계약·입력 지원 범위·한계.
- `handoff.md`: 전체 미커밋 파일을 빠짐없이 통합하고, 프롬프트 수정 이력은 ‘한 일’, 유지해야 할 동작·경계는 ‘다음 사람이 알아야 할 것’, 미검증은 ‘다음 작업’에 구분했다.
