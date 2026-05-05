# PRD: K-SpamGuard
**버전 0.3 · 2026-05-05 · 상태: 초안**

---

## 목차
1. [Executive Summary](#1-executive-summary)
2. [문제 정의](#2-문제-정의)
3. [사용자 페르소나](#3-사용자-페르소나)
4. [기능 목록 및 우선순위](#4-기능-목록-및-우선순위)
5. [MVP 범위](#5-mvp-범위)
6. [상세 기능 명세](#6-상세-기능-명세)
7. [구독 플랜](#7-구독-플랜)
8. [리스크 레지스터](#8-리스크-레지스터)
9. [포트폴리오 기술 포인트](#9-포트폴리오-기술-포인트)
10. [성공 지표](#10-성공-지표)
11. [개발 페이즈](#11-개발-페이즈)
12. [미결 사항](#12-미결-사항)

---

## 1. Executive Summary

K-SpamGuard는 한국어 인스타그램 계정에서 발생하는 변형 스팸 댓글을 자동으로 탐지·관리하는 B2B SaaS다.

**핵심 가치 제안**: 하루 30분 이상 소요되는 스팸 댓글 수동 관리를 5분 이하로 줄인다.

**1차 목표 (포트폴리오)**: 금융권 수준의 보안·감사 로깅·외부 API 신뢰성을 Spring Boot 프로젝트로 증명.
**2차 목표 (SaaS)**: 한국 크리에이터·소상공인을 대상으로 월 9,900원~49,900원 구독 수익 확보.

---

## 2. 문제 정의

### 2.1 한국어 스팸의 특수성

Instagram 기본 필터는 영어 중심이다. 한국어 스팸은 아래 기법으로 필터를 우회한다.

| 변형 유형 | 예시 | 단순 키워드로 탐지 여부 |
|---|---|---|
| 특수문자 삽입 | `카지노 → 카·지·노`, `대출 → 대.출` | 불가 |
| 초성 분리 | `스팸 → ㅅㅍㅇ`, `도박 → ㄷㅂ` | 불가 |
| 숫자 치환 | `영상 → 영0`, `일삼칠 → 137` | 불가 |
| 이모지 혼입 | `광고💰합니다`, `부업✅가능` | 불가 |
| URL 변형 | `bit.ly`, `t.me`, `카카오 ID` 삽입 | 부분 가능 |
| 반각/전각 혼용 | `Ｃａｓｉｎｏ` | 불가 |

### 2.2 비즈니스 임팩트

- **브랜드 신뢰도**: 스팸 댓글이 팔로워에게 노출되면 계정 신뢰도 하락
- **운영 비용**: 댓글 관리에 하루 20~60분 소요 (팔로워 10만+ 기준)
- **대응 지연**: 야간·주말 스팸 파동 시 대응 불가
- **법적 리스크**: 도박·대출 광고 방치 시 계정 신고 위험

### 2.3 기존 대안의 한계

| 대안 | 한계 |
|---|---|
| Instagram 기본 필터 | 영어 기반, 한국어 변형 패턴 미지원 |
| 수동 삭제 | 시간 소모, 24/7 대응 불가 |
| 범용 소셜 관리 도구 (Hootsuite 등) | 한국어 특화 없음, 고가, 자동 삭제 미지원 |
| 직접 스크립트 개발 | 기술 진입장벽, 유지보수 부담 |

---

## 3. 사용자 페르소나

### Persona A — 뷰티 인플루언서 (핵심 타겟)
- **이름**: 김지수, 27세
- **규모**: 팔로워 8만, 일 평균 댓글 200개, 스팸 비율 15~30%
- **페인 포인트**: 매일 아침 30분을 스팸 삭제에 씀. 광고 게시 당일 스팸 폭주.
- **원하는 것**: 자동으로 처리되되, 내가 검수할 수 있는 구조. 오탐은 절대 싫음.
- **지불 의향**: 월 1~2만원. 시간 절약이 명확하면 결제함.
- **사용 패턴**: 하루 1회 대시보드 접속, 검수 큐 확인 5분.

### Persona B — 쇼핑몰 운영자 (중요 타겟)
- **이름**: 박민준, 35세, 의류 쇼핑몰 운영
- **규모**: 계정 3개, 합산 팔로워 25만, 마케팅 담당 직원 1명
- **페인 포인트**: 직원이 댓글 관리에 시간을 쓰는 것이 비효율. 경쟁사 악의적 스팸 의심.
- **원하는 것**: 다중 계정 통합 관리, 자동 처리, 주간 리포트.
- **지불 의향**: 월 3~5만원. 팀 단위로 사용할 수 있으면 더 낼 의향 있음.
- **사용 패턴**: 주 2회 리포트 확인, 직원이 매일 큐 검수.

### Persona C — 소규모 로컬 브랜드 (진입 타겟)
- **이름**: 이서연, 42세, 카페 운영
- **규모**: 팔로워 3,000, 스팸은 가끔이지만 갑자기 증가하는 경우 있음
- **페인 포인트**: IT에 익숙하지 않음. 복잡한 설정 싫음.
- **원하는 것**: 설치하면 알아서 돌아가는 것. Free 플랜으로 시작.
- **지불 의향**: 무료로 쓰다가 효과 확인 후 유료 전환.
- **사용 패턴**: 알림 받을 때만 접속.

---

## 4. 기능 목록 및 우선순위

> **P0** = MVP 필수 (없으면 제품이 안 됨)
> **P1** = MVP+1 (유료 전환의 핵심 동인)
> **P2** = 성장 단계 (Retention 강화)

| # | 기능 | 우선순위 | 근거 |
|---|---|---|---|
| F-01 | Instagram OAuth 계정 연동 | **P0** | 모든 기능의 전제 조건 |
| F-02 | Webhook 수신 + 멱등성 처리 | **P0** | 댓글 수집의 기반 |
| F-03 | 한국어 정규화 파이프라인 | **P0** | 핵심 차별화 기능 |
| F-04 | 룰 기반 스팸 탐지 엔진 | **P0** | 핵심 차별화 기능 |
| F-05 | Moderation Queue (검수 큐) | **P0** | 사용자가 결과를 검수하는 핵심 UX |
| F-06 | 댓글 숨김/삭제 액션 (비동기) | **P0** | 서비스의 최종 가치 전달 |
| F-07 | Audit Log (불변) | **P0** | 금융권 포트폴리오 요구사항 + 신뢰성 |
| F-08 | 기본 대시보드 (큐 + 통계) | **P0** | 사용자 접점 |
| F-09 | 자동 모드 (임계값 이상 자동 승인) | **P1** | 유료 플랜 핵심 기능, Persona A 결제 동인 |
| F-10 | 기본 제공 룰셋 (도박/대출/성인/피싱) | **P1** | 온보딩 마찰 감소 |
| F-11 | 구독 플랜 + 사용량 한도 | **P1** | 수익화 필수 |
| F-12 | 다중 계정 지원 | **P1** | Persona B 결제 동인 |
| F-13 | 폴링 백업 수집 (Webhook 보완) | **P1** | 안정성 — Webhook 누락 방어 |
| F-14 | OAuth 토큰 자동 갱신 | **P1** | 60일 만료 대응, 운영 필수 |
| F-15 | 커스텀 룰 CRUD | **P1** | Persona B가 직접 키워드 관리 필요 |
| F-16 | 주간/월간 리포트 이메일 | **P2** | Retention 강화 |
| F-17 | Audit Log 내보내기 (CSV) | **P2** | 기업 고객 요구 |
| F-18 | 스팸 FP/FN 피드백 → 룰 개선 제안 | **P2** | 탐지 정확도 지속 개선 |
| F-19 | 팀 멤버 초대 (계정 공유) | **P2** | Persona B 팀 사용 |
| F-20 | 이메일/앱 알림 (스팸 폭주 감지) | **P2** | 야간 대응 |

---

## 5. MVP 범위

### IN (P0 전체 + 핵심 P1)

```
계정 연동 (1개) → Webhook 수신 → 정규화 파이프라인 → 룰 엔진
→ Moderation Queue → 숨김/삭제 → Audit Log → 대시보드
+ 자동 모드 (P1, Starter 플랜 차별화)
+ 기본 룰셋 제공 (P1, 온보딩 마찰 감소)
+ 구독 플랜 (P1, 수익화)
```

### OUT (MVP 제외)

| 기능 | 제외 이유 |
|---|---|
| 다중 계정 (3개+) | 연동 복잡도 증가, Starter 플랜에서 검증 후 구현 |
| 폴링 백업 | Webhook 안정성 검증 후 추가 |
| 토큰 자동 갱신 | 수동 재연동으로 MVP 커버, 운영 사이클에서 추가 |
| 주간 리포트 이메일 | Retention 기능, 핵심 가치 확인 후 추가 |
| 팀 멤버 초대 | 멀티 테넌시 복잡도, Pro 플랜 단계에서 구현 |
| ML 기반 분류 | 룰 엔진으로 MVP 정확도 충분, 데이터 축적 후 고려 |
| 모바일 앱 | 관리자 대시보드는 데스크탑 충분 |
| 다국어 지원 | 한국어 특화가 차별화 포인트 |
| 결제 (국내 PG) | MVP는 Stripe로 단순화. 국내 PG는 SaaS 성장 단계에서 추가 |

---

## 6. 상세 기능 명세

### F-01. Instagram OAuth 계정 연동

**유저 스토리**: 인스타그램 비즈니스/크리에이터 계정 소유자가 K-SpamGuard에 계정을 연동하면 Webhook이 자동 등록된다.

**플로우**:
1. 사용자가 "계정 연동" 클릭
2. Meta OAuth 동의 화면 (`instagram_business_basic`, `instagram_business_manage_comments` 스코프 요청)
3. 액세스 토큰 수신 → AES-256-GCM 암호화 → DB 저장
4. Webhook 구독 자동 등록 (`comments` 필드)
5. 연동 완료 화면, Audit Log: `account_connect` 기록

**예외 처리**:
- 토큰 저장 실패 시 연동 롤백, 사용자에게 에러 표시
- Business/Creator 계정이 아닌 경우 안내 메시지

---

### F-02. Webhook 수신 + 멱등성

**플로우**:
1. `POST /webhook/instagram` 수신
2. `X-Hub-Signature-256` HMAC-SHA256 검증 (실패 시 400 반환)
3. Redis에서 `ig_comment_id` 중복 확인 (TTL 24h)
4. 중복이면 즉시 200 반환 (silent drop)
5. 신규면 Comment 저장 (`status=PENDING`) → 탐지 큐 적재

**멱등성 보장**: Instagram은 동일 이벤트를 최대 3회 재발송할 수 있음. Redis key로 중복 방어.

---

### F-03 + F-04. 정규화 파이프라인 + 룰 엔진

**정규화 파이프라인 (순서대로 적용)**:

| 단계 | 처리 내용 | 예시 |
|---|---|---|
| 1. NFKC 정규화 | 전각→반각, Unicode 통합 | `Ａ→A`, `①→1` |
| 2. 특수문자 제거 | 허용 목록 외 제거 (한글, 영숫자, 공백만 허용) | `카·지·노→카지노` |
| 3. 숫자→문자 치환 | 0→o, 1→l/i, 3→e, 7→t | `0nlyfans→onlyfans` |
| 4. 초성 확장 | 단독 자음 조합 패턴 탐지 | `ㄷㅂ→도박` |
| 5. URL 추출 | http/https, bit.ly, t.me, 단축 URL 추출 | 별도 URL 블랙리스트 대조 |

**룰 엔진**:
- 각 SpamRule은 `pattern`, `weight`, `category`, `is_active` 보유
- 정규화된 텍스트에 모든 활성 룰 적용
- `score = Σ(matched_rule.weight)`
- `score ≥ threshold` → SPAM, `score < threshold` → HAM
- `threshold * 0.6 ≤ score < threshold` → UNCERTAIN (검수 큐 적재)

**기본 제공 룰셋 카테고리**: 도박, 대출/금융, 성인, 피싱/스캠, URL 스팸

---

### F-05. Moderation Queue (검수 큐)

**큐 적재 조건**:
- SPAM 판정 댓글 (자동 모드 OFF)
- UNCERTAIN 판정 댓글 (항상)
- SPAM 판정 + 자동 모드 ON → 자동 승인 큐로 바로 처리

**대시보드 UX**:
- 카드형 리스트: 원문 텍스트 + 정규화 텍스트 + 스팸 스코어 + 매칭된 룰 이름
- 인라인 액션 버튼: **승인(숨김)** / **승인(삭제)** / **거부(정상)**
- 필터: 상태(대기/완료/거부), 스코어 범위, 날짜, 계정
- 벌크 선택 → 일괄 승인/거부

---

### F-06. Instagram 댓글 액션 (비동기)

**플로우**:
1. 사용자 승인 → `ModerationAction` 생성 (`status=PENDING`, `idempotency_key=UUID`)
2. 구독 한도 확인: `usageCounter.month >= plan.limit` → 차단 + 안내
3. Redis Queue 적재
4. Worker: Queue 소비 → Instagram Graph API 호출
   - 숨김: `POST /{comment-id}` `{"hidden": true}`
   - 삭제: `DELETE /{comment-id}`
5. 성공: `status=COMPLETED`, `usageCounter++`
6. 실패: Exponential Backoff 재시도 (1s, 2s, 4s, 최대 3회)
7. 최종 실패: `status=FAILED`, 사용자 알림 (P2)

**Circuit Breaker** (Resilience4j):
- 슬라이딩 윈도우 10회 중 5회 실패 → OPEN (30초)
- OPEN 상태에서 요청 → 즉시 실패, 큐에 재적재

**멱등성**: `idempotency_key`로 같은 액션 중복 실행 방지. Worker가 재시작되어도 동일 댓글에 두 번 hide/delete하지 않음.

---

### F-07. Audit Log

**원칙**: INSERT ONLY. UPDATE/DELETE 경로 없음. 서비스 레이어에서 비즈니스 트랜잭션과 **분리된 트랜잭션**으로 기록 (비즈니스 롤백이 감사 로그를 삭제하지 않도록).

| 이벤트 | 기록 항목 |
|---|---|
| `login_success` / `login_failure` | user_id, ip, user_agent, timestamp |
| `account_connect` | user_id, ig_account_id, timestamp |
| `account_disconnect` | user_id, ig_account_id, timestamp |
| `rule_create` | user_id, rule_id, after_value(JSON), timestamp |
| `rule_update` | user_id, rule_id, before_value(JSON), after_value(JSON), timestamp |
| `rule_delete` | user_id, rule_id, before_value(JSON), timestamp |
| `moderation_approve` | user_id, comment_id, action_type, timestamp |
| `moderation_reject` | user_id, comment_id, timestamp |
| `token_refresh` | user_id, ig_account_id, timestamp |
| `plan_change` | user_id, from_plan, to_plan, timestamp |

**보존 기간**: Free 30일, Starter 90일, Pro 1년.

---

### F-09. 자동 모드

**설정**: 계정별로 `auto_threshold` 설정 (0.0~1.0, 기본 0.9)
- `spam_score ≥ auto_threshold` → 사용자 검수 없이 자동 승인(숨김)
- `auto_threshold` 이하 → 검수 큐로 이동

**리스크 완화**: 자동 모드도 Audit Log 기록. 사용자는 "자동 처리된 댓글" 탭에서 사후 확인 가능. 오탐 발견 시 "복구" 액션 제공 (숨김 해제 → Instagram API `hidden=false`).

---

## 7. 구독 플랜

| | **Free** | **Starter** | **Pro** |
|---|---|---|---|
| **월 요금** | 무료 | 9,900원 | 49,900원 |
| **월 처리 댓글** | 1,000건 | 10,000건 | 100,000건 |
| **연동 계정** | 1개 | 3개 | 10개 |
| **자동 모드** | X | O | O |
| **커스텀 룰** | 5개 | 50개 | 무제한 |
| **Audit Log 보존** | 30일 | 90일 | 1년 |
| **주간 리포트** | X | X | O |
| **팀 멤버** | 1명 | 1명 | 5명 |

**한도 초과 동작**: 액션 차단 + 대시보드 경고. 다음 달 1일 자동 리셋.
**업그레이드 유도**: 한도 80% 도달 시 배너 노출.

---

## 8. 리스크 레지스터

| ID | 리스크 | 확률 | 영향 | 완화 전략 |
|---|---|---|---|---|
| R-01 | **Instagram App Review 거절** | 높음 | 치명적 | 포트폴리오 MVP는 Mock Adapter로 구현하고, Real Adapter는 개발 모드 + 역할 부여된 Business/Creator 테스트 계정으로 검증한다. 앱 심사는 사용자 10명+ 확보 후 신청. 심사 전까지 초대 기반 베타 운영. |
| R-02 | **오탐(FP) 으로 인한 정상 댓글 숨김** | 중간 | 높음 | 자동 모드 threshold 기본값 0.9로 높게 설정. 자동 처리 내역 사후 확인 + 복구 기능 필수. UNCERTAIN 범위는 항상 검수 큐로. |
| R-03 | **OAuth 토큰 탈취** | 낮음 | 치명적 | AES-256-GCM 암호화 저장. 복호화 키는 환경 변수만. DB 탈취만으로 토큰 사용 불가. |
| R-04 | **Instagram API Rate Limit 초과** | 중간 | 중간 | 계정당 시간당 200건 추적 (Redis 카운터). 한도 80% 도달 시 Worker 속도 조절. 초과 시 다음 시간대로 큐 지연 처리. |
| R-05 | **Webhook 미수신 / 지연** | 중간 | 중간 | P1에서 폴링 백업 구현 (1시간 주기). Webhook 수신률 모니터링 지표 추가. |
| R-06 | **결제 이탈 (Stripe + 한국 카드 호환)** | 중간 | 중간 | MVP는 Stripe만 (글로벌 카드). 국내 PG(토스페이먼츠)는 사용자 50명+ 이후 추가. |
| R-07 | **스팸 패턴 진화** | 높음 | 낮음 | 룰 엔진은 사용자가 직접 업데이트 가능. FP/FN 피드백 루프(P2)로 룰 개선 반영. |
| R-08 | **서비스 중단 시 미처리 큐 유실** | 낮음 | 중간 | Redis 영속성 설정 (AOF). Worker 재시작 시 PENDING 상태 액션 자동 재처리. |

---

## 9. 포트폴리오 기술 포인트

금융권 채용 관점에서 어필할 구현 포인트.

### 9.1 외부 API 신뢰성 패턴
```
Instagram API 호출 실패 시나리오 대응:
  Circuit Breaker (Resilience4j) → 연속 실패 시 빠른 실패
  Retry + Exponential Backoff → 일시적 장애 자동 복구
  Idempotency Key → 재시도 시 중복 액션 방지
  Rate Limit Tracking → Instagram API 한도 사전 방어
```
**어필 포인트**: "외부 API를 신뢰하지 않는 설계. 모든 실패 케이스를 테스트로 검증."

### 9.2 보안 설계
```
OAuth 토큰: AES-256-GCM + per-token IV → DB 탈취만으로 토큰 사용 불가
외부 ID: UUID → 내부 PK 순서 노출 없음 (IDOR 방어)
Webhook 검증: HMAC-SHA256 서명 검증 → 위변조 요청 차단
Rate Limiting: Bucket4j → API 남용 방어
```
**어필 포인트**: "OWASP ASVS L2 기준 보안 체크리스트 적용."

### 9.3 감사 가능성 (Auditability)
```
모든 상태 변경 → Audit Log (INSERT ONLY)
비즈니스 트랜잭션 롤백 → 감사 로그는 보존
구독 한도 초과 → 액션 차단 기록
```
**어필 포인트**: "금융 시스템에서 요구하는 불변 감사 추적(immutable audit trail) 구현."

### 9.4 테스트 전략
```
단위 테스트: NormalizationPipeline, RuleEngine — 순수 도메인 로직
통합 테스트: Testcontainers PostgreSQL — 실제 DB 대상 트랜잭션 검증
Mock 어댑터: FakeInstagramAdapter — 실제 API 호출 없이 실패/재시도 시나리오 테스트
멱등성 테스트: 동일 Webhook 3회 수신 → 1회만 처리되는지 검증
```
**어필 포인트**: "외부 의존성을 포트/어댑터로 분리해 테스트 가능한 설계."

### 9.5 Hexagonal Architecture
```
domain/      순수 비즈니스 로직 (Spring 의존 없음)
application/ 유스케이스 + Port 인터페이스
infrastructure/ JPA, Redis, Instagram API 어댑터
presentation/   REST Controller, Webhook Handler
```
**어필 포인트**: "Instagram API 어댑터를 교체해도 도메인 로직 변경 없음. 테스트 어댑터와 프로덕션 어댑터를 같은 포트로 교체 가능."

---

## 10. 성공 지표

### 10.1 기술 지표 (포트폴리오)

| 지표 | 목표 | 측정 방법 |
|---|---|---|
| 스팸 탐지 Precision | ≥ 90% | eval/spam-samples.json 기준 |
| 스팸 탐지 Recall | ≥ 85% | eval/spam-samples.json 기준 |
| Webhook 처리 지연 | p99 ≤ 2초 | Micrometer + CloudWatch |
| Instagram API 액션 최종 성공률 | ≥ 99% | 재시도 포함, ModerationAction 완료율 |
| 중복 액션 발생 | 0건 | idempotency_key 충돌 카운터 |
| Audit Log 누락 | 0건 | 이벤트 발생 수 vs 로그 수 대조 |
| 단위 테스트 커버리지 (도메인) | ≥ 80% | JaCoCo |

### 10.2 제품 지표 (SaaS)

| 지표 | 3개월 목표 | 6개월 목표 |
|---|---|---|
| 가입 사용자 | 50명 | 200명 |
| 유료 전환율 | 20% | 30% |
| MRR | 50만원 | 300만원 |
| 월 이탈률 | < 10% | < 7% |
| NPS | - | ≥ 30 |

---

## 11. 개발 페이즈

### Phase 1 — MVP (P0 기능)
**목표**: 1개 계정으로 스팸 탐지 → 검수 큐 → 숨김/삭제 동작하는 프로토타입
**기간**: 6~8주

포함:
- Spring Boot 프로젝트 셋업 (Hexagonal 구조, Flyway, Testcontainers)
- Instagram OAuth + Webhook 수신
- 정규화 파이프라인 + 룰 엔진 (기본 룰셋)
- Moderation Queue + 비동기 액션
- Audit Log
- 최소 대시보드 (Next.js, 큐 리스트 + 통계)

### Phase 2 — SaaS 기반 (핵심 P1)
**목표**: 유료 구독 가능한 상태. 베타 사용자 10명 확보
**기간**: 4~6주

포함:
- 자동 모드 + threshold 설정
- 구독 플랜 + Stripe 결제
- 다중 계정 지원
- 커스텀 룰 CRUD
- OAuth 토큰 자동 갱신
- 폴링 백업 수집

### Phase 3 — 성장 (P2)
**목표**: Retention 강화, 팀 기능, 데이터 활용
**기간**: 지속

포함:
- 주간 리포트 이메일
- FP/FN 피드백 → 룰 개선 제안
- 팀 멤버 초대
- Audit Log CSV 내보내기
- 알림 (스팸 폭주 감지)

---

## 12. 미결 사항

| # | 질문 | 결정 기한 | 영향 범위 |
|---|---|---|---|
| Q-01 | Instagram App Review 신청 타이밍: 베타 사용자 몇 명부터? | Phase 1 완료 전 | R-01 완화 전략 |
| Q-02 | Free 플랜 Audit Log 30일 보존이 규정 준수에 충분한가? | Phase 2 전 | DB 스토리지 비용, 프리미엄 업그레이드 유인 |
| Q-03 | UNCERTAIN 댓글 자동 처리 여부: 검수 큐만 vs 낮은 threshold 자동 숨김 옵션 제공? | Phase 1 UX 설계 | FP 리스크 |
| Q-04 | Stripe 외 국내 PG (토스페이먼츠) 추가 시점 | Phase 2 검토 | 전환율 영향 |
| Q-05 | 서비스 이름/도메인 확보 — k-spamguard.io vs 한국어 도메인 | Phase 2 전 | 브랜딩 |
