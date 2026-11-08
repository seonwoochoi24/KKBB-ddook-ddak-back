# 깨비뚝딱 작업 인수인계

기준일: 2026-11-08. PR 브랜치 `feat/situation-menu-recommendation`, 변경 비교 기준 `origin/main`의 `208b757`에서 이번 작업을 정리했다. 사용자의 PR 생성 요청에 따라 변경을 커밋·브랜치 푸시·PR로 공유하며, main 병합·배포는 별도다. 현재 상태와 최신 기록을 먼저 읽고, 2026-11-06 기록은 당시 구현 이력으로 참고한다.

## 현재 상태

| 항목 | 상태 |
|---|---|
| 작업 위치 | `KKBB-ddook-ddak-back` 저장소 루트 |
| 저장소·브랜치 | origin `https://github.com/seonwoochoi24/KKBB-ddook-ddak-back.git`, `feat/situation-menu-recommendation`, 변경 비교 기준 `origin/main`의 `208b757` |
| 저장 상태 | 이번 상황 기반 JSON 메뉴 추천·로컬 의미검색·테스트·실험 결과를 PR에 포함. 실제 커밋 SHA와 PR 상태는 Git 이력·GitHub에서 확인 |
| 메뉴 데이터 구축 | **KB스타뱅킹 전체 메뉴를 JSON 카탈로그로 구성**. 제공된 앱 화면·사용자 확인 범위에서 기능별 중복을 정리한628개 항목. 전체 내부 화면의 완전 검증을 뜻하지 않음 |
| 기본 API | `POST /api/canvas`: 상황 → 업무 판단 → 의미검색 → 메뉴 선택. 글자검색과 글자검색 폴백 없음 |
| 추천 범위 | 원본 JSON 메뉴628개. 정의된 업무54개와 명확한 기타 메뉴 요청 경로에서 최대3개 업무 판단, 업무별 메뉴1개. 중복 메뉴는 합침 |
| JEV 호출 | 최대2회: 업무 판단1회, 업무별 메뉴·요청구분을 묶어 선택1회. 빈 입력0회, 조기 보류1회 |
| 검색 | 로컬 multilingual-e5-small ONNX CPU 모델. 문서 벡터는 미리 계산·캐시, 요청 시 업무 검색문 벡터 계산. DB·외부 임베딩 API 미사용 |
| 반환 | `greeting/status/menus/recommendations/selection_metrics/decision_notes/widgets`. `widgets`는 빈 배열. 실제 위젯 데이터 생성·거래 실행 없음 |
| 이전 실험 API | `/api/experiments/widget-selection`은 고정9개 위젯의 jev/OpenAI 선택 비교용으로 남음. 새 상황 추천을 시험하는 API가 아님 |
| 검증 | PR 직전 실제 프로젝트에서 `./mvnw -o test` 39개 통과(실패·오류·건너뜀0), `git diff --check` 통과 |
| 실제 모델 검증 | 상황 방식 수정 전50문항 비교 완료. 이후 지원금 요청 버그 수정은 같은 문장 실제3회만 재확인. 최신 코드 전체50문항 재평가는 아직 없음 |
| 프론트·배포 | 이번 작업에서 프론트 수정·전체 렌더링 검증·원격 배포 미수행. 기존 위젯 전용 프론트는 `menus/recommendations` 렌더링 연결 필요 |
| 협업·발표 | 각자 포크에서 개발한 결과 중 발표용 개선을 선별하는 기존 방향 유지. 팀 공통 구현·발표 채택·원본 복구 완료는 이번 작업에서 확정하지 않음 |

## 다음 작업

| 순서 | 할 일 | 확인할 결과 |
|---|---|---|
| 1 | 최신 지원금 요청 수정까지 포함해 전체50문항 재실험 | 기존44/50 수치를 최신 코드 정확도로 그대로 인용하지 않기. 계획 보류·메뉴 보류·태그 불확실·추가 제안 제외를 구분 |
| 2 | 직접 요청의 회귀 확인 | 통장사본·예금잔액증명서·복수 업무, 지갑 분실·사업자 지원 상황 재검증. 표본에 맞춘 수정 후에는 새 질문으로 별도 평가 |
| 3 | 실행 환경·실제 프론트 연동 확인 | 로컬 의미검색 준비·백엔드 재시작, `menus/recommendations` 최대3개 표시, `basis=uncertain`, `decision_notes`, partial·오류 표시 |
| 4 | 상황·업무 정의 범위 점검 |54개 업무 밖의 요청·잘못된 추가 제안·제외 조건 평가. 상황 제안은 유일한 금융 정답이나 가입 가능 판정이 아님 |
| 5 | 비교 기준 버전 확보 및 발표 정리 | 동일 질문·동일 모델·동일 설정으로 비교. 기준 구현이 서로 다른 서버를 사용해야 하며 현재 코드를 포트만 바꿔 띄우면 과거 방식 비교가 아님 |
| 6 | 커밋 전 변경 검토 | 실험 JSON·스크립트·문서 포함 범위 확인. `.idea`, 키, `.semantic-runtime` 모델·벡터·가상환경은 커밋 제외. 사용자 PR 요청 범위에서 브랜치 커밋·푸시·PR 생성, main 병합·배포는 별도 |

## 작업 기록

### 2026-11-08 · PR 준비 검증

- 사용자가 PR 생성을 요청했다. 이전 작업 브랜치는 이미 main에 병합돼 있어 최신 origin/main의208b757에서 feat/situation-menu-recommendation을 분리했다.
- 실제 프로젝트의 오프라인 전체 테스트39개 통과, 실패·오류·건너뜀0. 모의 검증이며 실모델 전체50문항은 재실행하지 않았다.
- PR 대상 파일에서 자격증명·주민번호 패턴이 발견되지 않았고 모델·가상환경·벡터 캐시·IDE 설정은 Git 제외를 확인했다. 커밋 전 변경 검토는 실제 diff·미추적 파일을 포함한다.
- main 병합과 원격 배포는 수행 범위 밖이다.

### 2026-11-08 · 스타뱅킹 전체 메뉴 JSON 구축·상황 판단 추천

**먼저 한 일: 스타뱅킹 전체 메뉴를 JSON으로 구성**

- 스타뱅킹 메뉴를 AI가 선택할 수 있도록 앱 화면을 수집·분류하고, 기능별 중복을 합쳐 **전체 메뉴 카탈로그628개**를 작성했다. 기존 고정9개 위젯을 넘어 메뉴 데이터 자체를 구축한 작업이 이번 변경의 기반이다.
- 결과 파일은 `src/main/resources/kb_starbanking_menu_structure.json`이다. 각 항목은 `id`, `name`, `description`, `select_when`으로 구성하며, 메뉴 경로 없이 기능 설명과 사용 상황으로 선택할 수 있게 했다.
- 같은 기능의 반복 노출은 통합하고 개인/사업자·국내/해외·신청/조회/해제 등 대상이나 업무가 다르면 구분했다. 상황 추천은 이 원본 카탈로그의 실제 ID만 반환한다.
- 여기서 ‘전체 메뉴’는 **제공된 스타뱅킹 화면에서 수집한 메뉴 범위를 하나의 카탈로그로 정리했다는 뜻**이다. 접근 불가한 밀리터리 클럽, 버튼 이후 내부 화면 등은 미확인으로 남아 있어 앱의 모든 상태·조건별 하위 기능을 빠짐없이 검증했다고 주장하지 않는다.
- 캡처가 불가능했던 결제6개는 사용자 설명으로 확인한 예외다. 수집·출처 기록 원본은 저장소 밖 별도 수집 자료의 `kb_starbanking_menu_evidence.json`에 분리되어 있다. 원본 캡처·계좌번호·실명·잔액·거래내역은 메뉴 JSON에 포함하지 않았다.

**목적과 최종 흐름**

사용자는 단순히 메뉴명을 매칭하는 기능보다, 상황에서 필요한 업무를 찾아주는 기능을 원했다. 초기 글자검색 중심 구현을 최종 기본 경로에서 제외했다.

```text
사용자 상황 → POST /api/canvas
  → JEV 1차: situation_needs.json의 업무에서 최대3개 판단
  → 업무별 로컬 E5 의미검색
  → JEV 2차: 후보 중 메뉴 선택 + 직접요청/추가제안 구분(일괄)
  → 서버 ID 검증·중복 제거
  → menus / recommendations / decision_notes 반환
```

- 예: 일본 첫 해외여행 준비 → 환전·해외 QR 결제·여행보험을 검토할 업무로 제안. 이미 보험 가입, 환전만 요청 → 환전만 선택.
- 최대3개는 **추천 메뉴 수**이며 실제 위젯 호출·금융거래 수가 아니다. 실제 거래·가입·약관 동의·환율 계산을 실행하지 않는다.
- 업무1개면 의미검색 후보40개, 복수면 업무별12개. 상황 판단이 검색보다 먼저다. 로컬 검색 장애는503이며 글자검색으로 우회하지 않는다.
-54개의 업무 정의와 명확한 기타 메뉴용 경로를 쓰는 제한된 분류다. 무제한 자유 계획이나 사용자의 계좌·보유보험·자격 정보를 아는 구조가 아니다. 멀티턴 기억도 구현하지 않았다.

**핵심 파일**

| 파일 | 역할 |
|---|---|
| `service/JevCanvasService.java` | 현재 `/api/canvas` 응답 조립. `SituationMenuService`만 호출 |
| `service/SituationMenuService.java` | 업무 최대3개 판단, 의미검색, 메뉴·basis 선택, 확실성별 처리 |
| `service/JevDecisionClient.java` | JEV choice 요청과 응답·confidence·허용 선택지 검증 |
| `service/SemanticMenuRetriever.java` | localhost 의미검색 전용. 해시·메뉴 ID 검증, 장애503 |
| `service/MenuCatalog.java` | JSON628개 로드, 중복·개수·필수정보 검증, 원본 메뉴 반환 |
| `resources/kb_starbanking_menu_structure.json` | 원본 메뉴의 `id/name/description/select_when`. 앱 캡처에 근거한 메뉴 선택 자료 |
| `resources/situation_needs.json` | 상황에서 판단할 업무54개와 업무별 의미 검색문 |
| `resources/menu_selection_metadata.json` | hub/task/product/content 및 업무 동사 구분. 원본 메뉴 스키마와 분리 |
| `scripts/semantic/` | 모델 설치·고정 버전/해시·로컬 검색 서버·실행 안내 |
| `service/JevMenuSelectionService.java`, `service/HybridMenuRetriever.java` | 이전 단일·혼합검색 비교용 deprecated 클래스. Spring 서비스 등록 해제. 기본 canvas에서 호출하지 않음 |
| `docs/JEV_CANVAS.md`, `README.md` | 현재 API 계약·설계·실행·검증 범위 |

위 `service/` 경로의 루트는 `src/main/java/com/example/kb_ai_backend/`, `resources/`는 `src/main/resources/`다. 신규 테스트는 기존 비교 테스트에 더해 `SituationMenuServiceTest`, `SemanticMenuRetrieverTest`, `JevDecisionClientTest`, `HybridMenuRetrieverTest` 등에 있다.

**구현 과정에서 확인한 JEV 제한**

- 한 choice 질문의 선택지는 최대255개. 메뉴628개와 없음·확인 필요를 한 질문에 넣으면422로 실패했다.
- 선택지를 묶어도 전체 메뉴 설명을 한꺼번에 전송하면 입력량 초과400(`max_tokens_exceeded`)가 발생했다. 정확한 최대 토큰 수는 오류에서 확인되지 않았다.
- 초기에는 글자검색 후보40개, 다음에는 로컬 의미검색 혼합을 실험했으나, 최종 목적에 맞춰 업무 판단 후 의미검색만 쓰도록 바꿨다.

**로컬 E5 구성과 재개**

- 모델: `intfloat/multilingual-e5-small`, 고정 revision `614241f622f53c4eeff9890bdc4f31cfecc418b3`, int8 ONNX CPU 실행.
- 문서는 `passage:`, 질문은 `query:` 접두어, attention mask 기반 평균 pooling·L2 정규화. 모델·토크나이저 체크섬을 확인한다.
- 문서 벡터 캐시는 카탈로그 SHA-256과 모델 revision 기준. 모델·문서 벡터 로딩 시간은 아래 응답 시간에서 제외됐고, 요청별 질문 벡터 생성 시간은 포함됐다.
- `.semantic-runtime/`에 모델·벡터·가상환경 설치. 이 폴더는 Git에서 제외되므로 다른 환경에서는 setup이 필요하다. 작업 중 localhost:18083 검색 프로세스를 실행·health 확인했으나 계속 살아 있다고 가정하지 말고 재개 시 확인한다.
- `JEV_AI_API_KEY` 필요, `JEV_MODEL` 기본 `jev-latest`, `JEV_MIN_CONFIDENCE` 기본0.6. 임계값이 통계적으로 보정된 정답 확률은 아니다. 키 값은 문서·로그·보고서에 저장하지 않는다.
- `MENU_SEMANTIC_URL` 기본 `http://127.0.0.1:18083/search`, 루프백 HTTP만 허용. `MENU_RETRIEVAL_MODE`는 과거 비교용 설정이며 현재 canvas에서 글자검색을 켜는 옵션이 아니다.

```bash
# 다른 환경의 최초 설치: 공개 모델 다운로드 및 가상환경 생성
python3 scripts/semantic/setup.py
# 별도 터미널에서 로컬 의미검색 프로세스 실행
sh scripts/semantic/start.sh
# 준비 상태 확인: 모델·revision·카탈로그 해시·628개 여부
curl -fsS http://127.0.0.1:18083/health
# IntelliJ에서 JEV 키 설정 후 백엔드를 재시작하거나
./mvnw spring-boot:run
# 새 기능은 이 API로 확인. Swagger의 /api/experiments/widget-selection과 혼동하지 않기
curl -sS http://localhost:8080/api/canvas -H 'Content-Type: application/json'   -d '{"message":"나 사장인데 지원금 받고 싶어"}'
```

**실험 결과 — 서로 다른 문항 구성은 직접 비교하지 않기**

| 평가 | 비교 결과 | 평균 응답 | 근거 |
|---|---|---|---|
| 초기 메뉴 선택30문항 |29/30, 96.7% |288ms | `docs/menu-selection-results.json` |
| 동일60문항·글자/구분정보/혼합검색 |기존54/60(90.0%), 구분정보55/60(91.7%), 혼합55/60(91.7%) |253.4 / 258.8 / 268.5ms | `docs/hybrid-selection-results.json` |
| 동일50문항·기존 단일 vs 상황 방식 |38/50(76.0%) →44/50(88.0%) |307 →463.9ms | `docs/situation-selection-results.json` |
| 위50문항의 상황20개 |9/20 →17/20 |305.1 →459ms | 같은 보고서의 `by_split` |
| 위50문항의 직접요청30개 |29/30 →27/30, 일부 회귀 |308.3 →467.1ms | 같은 보고서의 `by_split` |
| 최신 지원금 요청 수정 |동일 문장 실제3회 모두 ready·정책자금 맞춤추천 |개별 회귀 확인 | `docs/support-basis-fix.json` |

- 모두 합성 질문·각 방식1회 평가다. 운영 정확도·SLA로 확대하지 않는다. 질문은 모델 호출 전 고정했고 실행 순서를 교대했다.
- 혼합검색은 정답 후보 포함률44/49→45/49로 높였지만 구분정보만 보강한 방식보다 최종 정답이 늘지 않았다. `docs/hybrid-retrieval-audit.json`에 검색 순위를 보존했다.
- 상황50문항은 직접요청30개+새 상황20개. 복수 업무1개는 새 목표에 맞춰 기대 결과를 확인 요청에서 두 메뉴 선택으로 변경했다. 제안 묶음의 정답은 제품 기대 동작이며 유일한 금융적 정답을 뜻하지 않는다.
- **44/50은 아래 지원금 요청 버그를 수정하기 전 결과다. 최신 버전의 전체50문항 정확도는 미확정이다.**
- 실행 스크립트: `scripts/benchmark_menu_selection.py`, `benchmark_hybrid_selection.py`, `benchmark_situation_selection.py`. 문항은 같은 폴더의 `cases.json`, `hybrid-cases.json`, `situation-cases.json` 등에 있다. 기존9개 위젯 실험은 `benchmark_widget_selection.py`다.
- 실제 비교에는 서로 다른 구현의 서버가 필요하다. 기준 버전은 당시 별도 임시 작업본에서 실행했다. 임시 작업본은 재개 시 존재한다고 가정하지 않는다. 커밋된 결과·문항·코드 해시를 확인하고 기준 버전을 별도 확보한다.

**최신 버그: 지원금 요청이 빈 결과로 나옴**

입력 `나 사장인데 지원금 받고 싶어`에서 사업자 지원 업무0.98, 정책자금 맞춤추천 메뉴1.00으로 선택했지만 요청/제안 구분0.32 때문에 전체 `needs_clarification`을 반환했다. 추가 재현에서는 핵심 업무0.99인데 부가 상담 후보0.26 때문에 전체 보류되는 문제도 확인했다.

수정 내용:

1. 확실한 메뉴 선택은 요청/제안 태그의 낮은 신뢰도로 버리지 않는다. 태그만 `basis=uncertain`으로 표시하고 `menuConfidence/basisConfidence`를 각각 반환한다.
2. 핵심 업무가 확실하면 불확실한 추가 제안은 제외한다. 같은 업무가 추가 슬롯에 중복된 경우도 핵심 업무를 막지 않는다.
3. 핵심 업무·메뉴 자체가 불확실할 때는 기존처럼 확인 요청한다. 임계값을 낮추거나 글자검색을 추가한 수정이 아니다.
4. `decision_notes`에 단계·업무 ID·사유·선택값·신뢰도를 반환한다. `optional_need_omitted`, `basis_uncertain`, `low_menu_confidence` 등으로 무엇이 불확실했는지 구분한다. 원문·키·프롬프트·생성된 추론 과정은 반환하지 않는다.
5. 동일 문장 실제 JEV3회 모두 `ready`, `정책자금 맞춤추천` 반환. 관련 회귀 테스트를 포함해39개 통과. 이 결과는 실제 지원금 수령 가능·대상 자격 판정이 아니라 메뉴 연결 확인이다.

**프론트/API 계약**

- 정상 `status`: ready, partial, needs_clarification, no_match, empty. `menus`는 기존 메뉴 객체 목록, `recommendations`는 menu·needId·need·basis·explanation·menuConfidence·basisConfidence다.
- `basis`: explicit(직접 요청/원하는 결과가 명확), suggested(상황상 제안), uncertain(이 구분만 불확실). uncertain 자체로 메뉴를 숨기지 않는다.
- `widgets=[]`는 이전 키를 유지한 것일 뿐, 기존9개 위젯이 실행·생성된 것이 아니다. `menus/recommendations`를 표시해야 한다.
- `selection_metrics`: retrievalMode=semantic_only, planningMs, retrievalMs, selectionMs, jevCalls. 부가 판단 불확실과 핵심 메뉴 불확실을 `decision_notes`로 구분한다.
- 일부 메뉴만 확정하면 partial, 전부 불확실하면 확인 요청. 빈 입력은 AI 호출 없음. 입력 최대2000자.
- HTTP400 입력 초과, 502 JEV/응답 오류, 503 키 누락 또는 로컬 의미검색 장애, 504 JEV 연결 실패·시간 초과. 공급자 장애를 빈 메뉴의 정상 결과로 숨기지 않는다.
- `/api/experiments/widget-selection`의 provider/model/scope/widget decisions 응답은 이전 실험 계약이다. 여기서 `widgets=[]`가 나왔다고 새 상황 판단이 실패한 것으로 해석하지 않는다.

**커밋 전 점검과 검증 경계**

- 최종 버그 수정 로직의 격리 작업본에서 `./mvnw -o test`:39개 통과. 동일 소스를 프로젝트에 반영하고 `git diff --check`를 확인했다. 이후 사용자 PR 요청에 따라 실제 프로젝트에서 `./mvnw -o test`를 다시 실행해39개 통과(실패·오류·건너뜀0)를 확인했다.
- 모델 호출 정확도 검증과 모의/기동 테스트 통과는 다른 지표다. 최신 전체 정확도, 프론트 표시, 실제 금융기관 연결, 멀티턴, 운영 환경·장애 시연은 남아 있다.
- 커밋할 자료는 변경 코드·JSON 카탈로그/업무 정의·테스트·벤치마크 스크립트·문항·docs 결과·README·handoff다. `.semantic-runtime/`, `.idea/`, 키·원본 캡처·target는 제외한다. 새 파일은 아직 미추적 상태일 수 있으니 `git diff`만 보지 말고 `git status --short`도 확인한다.
- 후속 사용자 요청으로 이번 변경을 커밋·브랜치 푸시·PR 생성 범위까지 공유한다. 원격 main 병합·배포는 요청되지 않았다.

### 이전 기록 안내

아래2026-11-06 기록의20개 파일·22개 테스트·기본9개 위젯 생성·브랜치/HEAD·응답 계약은 당시 기준이다. 현재 동작·파일 수·검증 상태는 위2026-11-08 기록을 우선한다.

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
6. **협업 결정 반영**: 원본과 별도로 각자 개발하고 발표할 개선만 추리기로 한 현재 방향을 기록했다.

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
- 과거 실측 원본 `깨비뚝딱_JEV_초기비교.json`과 GPT 토큰 전후 비교 기록 `깨비뚝딱_토큰비교_결과.json`은 저장소 밖의 별도 실험 자료다. 이 두 파일은 현재 저장소의 미커밋 목록에 포함되지 않으며 이번에 복사하지 않았다.

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
