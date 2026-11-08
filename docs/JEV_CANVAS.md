# 상황 기반 메뉴 추천

## 기본 흐름

JevCanvasService는 SituationMenuService만 호출합니다. 기본 경로에서 MenuCatalog.candidates 또는 HybridMenuRetriever를 사용하지 않습니다. 이전 글자·혼합검색 서비스는 비교 기록을 위한 deprecated 클래스로 남겼으며 Spring 서비스 등록을 해제했습니다.

1. JEV가 사용자 상황과 목적을 situation_needs.json의54개 업무로 분류하고 최대3개를 우선순위대로 선택합니다. 명시적 다른 메뉴 요청은 other_explicit 경로에서 원문 의미검색을 사용할 수 있습니다. 무관하면 no_match, 업무를 특정하지 못하면 확인 요청입니다.
2. SemanticMenuRetriever가 판단된 업무의 semantic_query를 로컬 E5로 검색합니다. 업무1개면40개, 복수면 업무별12개 후보를 가져옵니다. 글자 일치 점수나 RRF를 사용하지 않습니다. 모델 서버가 없거나 해시·ID·응답이 유효하지 않으면503입니다.
3. JEV에 업무별 후보를 함께 보내 실제 메뉴와 explicit/suggested 구분을 한 호출로 선택합니다. JSON 후보 밖 ID, 비정상 confidence, 누락 응답을 거부합니다. 검토 필요 업무는 확인을 요청하고 일부만 연결되면 partial입니다.

기존 카탈로그628개와 각 메뉴의4개 필드는 유지합니다. 업무 정의는 제품 추천 범위를 표현한 별도 JSON입니다. 상황상 필요한 업무 제안은 실제 가입·보유 상태를 증명하지 않으며 이용 가능 국가·요금·조건을 보장하지 않습니다. 이미 완료·제외한 업무는 다시 추천하지 않도록 명시했고 대출·리볼빙은 자금 부족만으로 제안하지 않도록 했습니다.

## 응답

menus는 기존 메뉴 객체 목록, recommendations는 {menu,needId,need,basis,explanation} 목록입니다. basis는 explicit·suggested·uncertain입니다. 메뉴 신뢰도와 요청/제안 구분 신뢰도는 별도이며, 구분만 불확실하면 메뉴를 그대로 반환합니다. menuConfidence와 basisConfidence가 각각의 값을 표시합니다. 고정 템플릿 explanation은 전체 추론 과정을 생성한 설명이 아닙니다.

selection_metrics: retrievalMode=semantic_only, planningMs, retrievalMs, selectionMs, jevCalls. 빈 입력은0회, 조기 보류는1회, 정상 메뉴 연결은최대2회입니다.

상태: ready, partial, needs_clarification, no_match, empty. 입력 최대2000자. 키 누락503, 의미검색 장애503, JEV 응답 오류502, JEV 연결 실패·시간초과504. 모델과 임계값은 JEV_MODEL·JEV_MIN_CONFIDENCE를 사용합니다.

## 실행

python3 scripts/semantic/setup.py 후 별도 터미널에서 sh scripts/semantic/start.sh, IntelliJ의 Spring 백엔드를 실행합니다. 이미 설치한 모델·벡터 캐시는 .semantic-runtime에 있습니다. 임베딩은 로컬에서만 계산하고 JEV에는 공개 업무 정의·관련 메뉴·요청만 전달합니다.

MENU_RETRIEVAL_MODE와 MENU_SELECTION_ENRICHMENT는 이전 비교용 클래스 설정이며 새 캔버스의 글자검색을 켜는 옵션이 아닙니다.

## 평가

situation-selection-results.json에 같은50문항·각1회 비교를 저장했습니다. 이전 직접요청30개와 새 상황20개입니다. 복수업무1문항은 새 계약에 맞춰 정답을 두 메뉴로 변경했습니다.

이전 단일메뉴38/50(76.0%)·평균307ms, 상황방식44/50(88.0%)·463.9ms. 상황20개는9→17, 직접30개는29→27입니다. 회귀가 있어 전면 개선이라 주장하지 않습니다. 새로운 제안 묶음의 정답은 제품 기대 규칙이며 유일한 금융적 정답을 뜻하지 않습니다.

서로 다른 질문으로 평가한 이전30개·60개 결과와 이번50개 정확도를 직접 비교하지 않습니다. 모델 가중치 학습·변경은 없습니다.


## 사업자 지원 요청 보류 수정

나 사장인데 지원금 받고 싶어 요청은 정책자금 맞춤추천을 확실히 선택하고도 요청/제안 구분 신뢰도가 낮아 비어 있었습니다. 또한 독립적인 추가 업무 슬롯의 낮은 신뢰도가 확실한 핵심 업무까지 막았습니다. 부가 구분은 uncertain으로 표시하고, 불확실한 추가 제안은 제외합니다. 핵심 업무·메뉴의 불확실성 검사는 유지합니다.

decision_notes 항목은 stage·needId·reason·choice·confidence를 포함합니다. 예: planning/optional_need_omitted는 낮은 신뢰도의 추가 제안 제외, request_basis/basis_uncertain은 요청/제안 구분만 불확실함, menu_selection/low_menu_confidence는 메뉴 자체가 불확실함입니다. 원문·API 키·프롬프트·생성된 추론 과정을 반환하지 않습니다.

동일 문장 실제 JEV3회 모두 ready와 정책자금 맞춤추천을 반환했습니다. 전체 평가의 재실행은 아니며 이전 정확도를 갱신하지 않습니다. docs/support-basis-fix.json 참조.
