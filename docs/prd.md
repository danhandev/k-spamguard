# PRD: K-SpamGuard

## 1. Background

인스타그램 한국어 계정에서 스팸 댓글은 단순 텍스트가 아니라 다양한 변형 기법을 사용한다.

| 변형 유형 | 예시 |
|---|---|
| 특수문자 삽입 | `카지노→카·지·노`, `대출→대.출` |
| 초성 분리 | `스팸→ㅅㅍㅇ` |
| 숫자 치환 | `영상→영0`, `일삼칠→137` |
| 이모지 혼입 | `광고💰합니다` |
| URL 변형 | `bit.ly`, `t.me` 링크 혼입 |

Instagram Graph API의 기본 필터는 영어 기반이며 위 변형 패턴을 탐지하지 못한다.
소상공인·크리에이터는 하루 수십~수백 개의 스팸을 수동 삭제해야 하는 상황이다.

## 2. Target Users

### Primary
- 한국어 인스타그램 소상공인 (식당, 뷰티, 쇼핑몰)
- 팔로워 1만~100만 규모 크리에이터

### Secondary (포트폴리오 관점)
- 금융권 채용 담당자: 보안, 감사 로깅, 외부 API 신뢰성 검토

## 3. Core Features

### 3.1 계정 연동
- Instagram Business/Creator 계정 OAuth 2.0 연동
- 계정당 Webhook 구독 등록 (comments, mentions)
- 다중 계정 지원 (플랜에 따라 한도 상이)

### 3.2 댓글 수집
- Instagram Webhook으로 실시간 댓글 수신
- 폴링 백업: Webhook 미수신 시 1시간 주기 보완 수집
- 멱등성: 동일 댓글 중복 수신 시 무시 (idempotency key: `ig_comment_id`)

### 3.3 스팸 탐지
- **정규화 파이프라인**
  1. Unicode NFKC 정규화
  2. 특수문자 제거 (허용 목록 방식)
  3. 숫자→문자 치환 (0→o, 1→l 등)
  4. 초성 패턴 탐지
  5. URL 추출 및 단축 URL 해제
- **룰 엔진**
  - 사용자 정의 키워드 블랙리스트
  - 패턴 매칭 (Regex)
  - 점수 합산 방식: 각 룰에 weight 부여, 임계값 초과 시 SPAM 판정
  - 기본 제공 룰셋 (도박, 대출, 성인, 피싱 카테고리)

### 3.4 Moderation Queue
- 스팸 판정 댓글은 자동으로 Queue에 적재
- 대시보드에서 액션 선택: 승인(숨김/삭제) / 거부(정상 처리)
- 자동 모드: 사용자 설정으로 특정 임계값 이상은 자동 승인
- 액션은 비동기 실행 (Redis Queue → Worker)

### 3.5 Instagram 액션
- 댓글 숨김 (hide): Instagram Graph API `POST /{comment-id}` `hidden=true`
- 댓글 삭제 (delete): Instagram Graph API `DELETE /{comment-id}`
- 멱등성: 동일 댓글에 동일 액션 중복 실행 방지
- 실패 시 재시도: Exponential Backoff (최대 3회)
- Circuit Breaker: 연속 실패 5회 시 30초 차단

### 3.6 룰 관리
- 키워드/패턴 CRUD
- 룰별 weight, 카테고리, 활성화 여부 설정
- 룰 변경 시 Audit Log 기록

### 3.7 Audit Log
불변 로그. 삭제/수정 불가.

| 이벤트 | 기록 항목 |
|---|---|
| login | user_id, ip, timestamp, result |
| account_connect | user_id, ig_account_id, timestamp |
| account_disconnect | user_id, ig_account_id, timestamp |
| rule_create / rule_update / rule_delete | user_id, rule_id, before/after, timestamp |
| moderation_approve | user_id, comment_id, action, timestamp |
| moderation_reject | user_id, comment_id, timestamp |

### 3.8 구독 플랜
| 플랜 | 월 처리 댓글 | 계정 수 | 자동 모드 |
|---|---|---|---|
| Free | 1,000 | 1 | X |
| Starter | 10,000 | 3 | O |
| Pro | 100,000 | 10 | O |

한도 초과 시 액션 차단, 다음 달 초 리셋.

## 4. Non-Goals (v1)
- ML 기반 분류 (룰 엔진으로 충분)
- 다국어 지원 (한국어 특화)
- 모바일 앱
- 팔로워 분석, 해시태그 분석

## 5. Success Metrics

| 지표 | 목표 |
|---|---|
| 스팸 탐지 정밀도(Precision) | ≥ 90% |
| 스팸 탐지 재현율(Recall) | ≥ 85% |
| Webhook 처리 지연 | p99 ≤ 2초 |
| Instagram API 액션 성공률 | ≥ 99% (재시도 포함) |
| 중복 액션 발생 | 0건 |
| Audit Log 누락 | 0건 |

## 6. Constraints
- Instagram Graph API: 계정당 시간당 200 comment hide/delete 한도
- OAuth 토큰 유효기간: 60일 (자동 갱신 구현 필요)
- Webhook: HTTPS 엔드포인트 필수, Meta 검증 서명(X-Hub-Signature-256) 검증 필수

## 7. Open Questions
- [ ] 사용자 직접 가입 vs 초대 기반 베타 운영 여부
- [ ] Free 플랜에서 Audit Log 보존 기간 (30일 vs 90일)
- [ ] 탐지 결과 피드백(FP/FN 리포트)을 룰 개선에 자동 반영할지 여부
