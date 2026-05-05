# Threat Model: K-SpamGuard
**버전 0.1 · 2026-05-05 · 기준: OWASP ASVS L2**

---

## 개요

K-SpamGuard는 Instagram OAuth 토큰을 보관하고 사용자 대신 댓글을 숨기거나 삭제한다. 가장 민감한 자산은 OAuth 액세스 토큰이며, 탈취 시 공격자가 해당 Instagram 계정의 콘텐츠를 임의 조작할 수 있다.

**분석 범위**: MVP 백엔드 (Spring Boot API, Moderation Worker, PostgreSQL, Redis)

**신뢰 경계**:
- 외부 → Spring Boot API (HTTPS, JWT 인증)
- Spring Boot API → Instagram Graph API (HTTPS, OAuth 토큰)
- Instagram → Webhook Endpoint (HMAC-SHA256 서명)
- Spring Boot API → PostgreSQL / Redis (내부 네트워크)

---

## T-01. OAuth 토큰 탈취

### 위협
공격자가 Instagram OAuth 액세스 토큰을 획득해 대상 계정의 댓글을 임의 삭제하거나 개인 데이터를 조회한다.

### 공격 시나리오

| 경로 | 방법 |
|---|---|
| DB 직접 탈취 | PostgreSQL 데이터 파일/백업 획득 → `encrypted_access_token` 컬럼 추출 |
| 메모리 덤프 | 복호화 후 메모리상의 평문 토큰 추출 |
| 로그 노출 | 평문 토큰이 애플리케이션 로그에 기록된 경우 |
| 중간자(MitM) | TLS 미적용 내부 통신 구간에서 패킷 캡처 |

### 대응책

| 대응 | 구현 위치 |
|---|---|
| AES-256-GCM 암호화 저장 | `AesGcmTokenEncryptor` — per-token IV (12 bytes), 암호화 키는 환경 변수 전용 |
| 복호화 스코프 최소화 | 서비스 레이어에서만 복호화. 복호화된 토큰은 메모리에만 존재, 직렬화/전송 금지 |
| 로그 마스킹 | Logback 패턴 필터: `access_token`, `Authorization` 헤더, `token` 포함 필드 마스킹 |
| DB 컬럼 노출 최소화 | JPA 프로젝션에서 `encrypted_access_token` 컬럼을 API 응답 DTO에 포함 금지 |
| TLS 적용 | ALB → ECS 구간 포함 전 구간 HTTPS/TLS |
| Secret 관리 | 암호화 키는 AWS Secrets Manager 또는 환경 변수. 코드/Git 하드코딩 금지 |

### 잔여 위험
복호화 키 자체가 탈취된 경우 (AWS Secrets Manager 권한 탈취 등) 평문 토큰 노출 가능. AWS IAM 최소 권한 원칙과 CloudTrail 모니터링으로 완화.

---

## T-02. Webhook 위조

### 위협
공격자가 K-SpamGuard Webhook 엔드포인트에 위조된 요청을 전송해 존재하지 않는 댓글을 DB에 삽입하거나, 타 계정의 댓글 ID를 조작해 Moderation Queue를 오염시킨다.

### 공격 시나리오

```
POST /webhook/instagram
X-Hub-Signature-256: sha256={임의_값}
Body: {"entry":[{"changes":[{"value":{"object_id":"target_comment_id","text":"정상댓글","verb":"update"}}]}]}
```

서명 검증이 없으면 공격자가 임의 댓글 ID를 삽입해 해당 댓글에 대한 HIDE/DELETE를 유도할 수 있다.

### 대응책

| 대응 | 구현 위치 |
|---|---|
| HMAC-SHA256 서명 검증 | `InstagramWebhookController` — `X-Hub-Signature-256` 헤더 검증. 불일치 즉시 400 반환 |
| 타이밍 공격 방어 | `MessageDigest.isEqual()` (constant-time 비교) 사용 — 문자열 `==` 비교 금지 |
| Raw Body 검증 | Spring `@RequestBody byte[]`로 원본 바이트 수신 후 HMAC 계산. DTO 파싱 전 서명 검증 선행 |
| App Secret 보안 저장 | Instagram App Secret은 환경 변수 또는 Secrets Manager. 코드 미포함 |
| Webhook URL 비공개 | URL 자체를 예측 불가한 경로로 구성 (추가 방어 심층화) |

### 잔여 위험
App Secret이 유출된 경우 공격자가 유효한 서명을 생성할 수 있다. App Secret은 Git에 절대 포함하지 않으며, 유출 감지 시 즉시 Meta 개발자 콘솔에서 재발급.

---

## T-03. IDOR (Insecure Direct Object Reference)

### 위협
인증된 사용자가 자신이 소유하지 않은 다른 사용자의 댓글, Moderation Action, 감사 로그에 접근하거나 조작한다.

### 공격 시나리오

```
# 사용자 A가 사용자 B의 Moderation Action UUID를 추측/열거
POST /accounts/{B의_accountId}/moderation/{B의_actionId}/approve
Authorization: Bearer {A의_JWT}
```

UUID는 순서 예측이 어렵지만 다른 경로(공유 링크, 로그 노출 등)로 노출될 수 있다.

### 대응책

| 대응 | 구현 위치 |
|---|---|
| 서비스 레이어 소유권 검증 | 모든 리소스 조회/변경 전 `resource.ownerId == currentUserId` 검증. Spring Security만으로는 불충분 |
| 내부 PK 미노출 | 모든 외부 식별자는 UUID(`external_id`). Long PK는 응답 DTO에 포함 금지 |
| 계정 스코프 강제 | `GET /accounts/{accountId}/comments`에서 `accountId`가 현재 사용자 소유인지 검증 |
| 감사 로그 격리 | `GET /audit-logs`는 현재 사용자의 로그만 반환. 타 사용자 `logId` 조회 시 404 (존재 노출 방지) |

### 잔여 위험
소유권 검증 누락 케이스가 코드 리뷰에서 놓일 수 있다. 통합 테스트에서 크로스 사용자 접근 시도를 403 기대값으로 반드시 포함.

---

## T-04. ReDoS (Regular Expression Denial of Service)

### 위협
공격자가 정규화 파이프라인의 정규식 패턴을 역으로 이용해 지수적 백트래킹을 유발하는 댓글 텍스트를 Webhook으로 전송해 Worker 스레드를 점유한다.

### 공격 시나리오

룰 엔진이 초성 확장이나 반복 패턴 매칭에 중첩 수량자(`(a+)+`, `(.*a.*)+`)를 사용할 경우:

```
입력: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!"
패턴: (a+)+!  → 지수적 백트래킹 발생
```

Webhook은 인증 없이 수신 가능하므로 악의적 댓글로 Worker 처리 시간을 무한정 점유할 수 있다.

### 대응책

| 대응 | 구현 위치 |
|---|---|
| 안전한 정규식 패턴 설계 | 룰 엔진 패턴에서 중첩 수량자(`(a+)+`, `(.+)+`) 사용 금지. 리뷰 체크리스트 포함 |
| 입력 길이 제한 | Webhook 수신 시 `text` 필드 5,000자 초과 즉시 DROP (Instagram 댓글 실제 한도 2,200자) |
| 타임아웃 적용 | 단일 댓글 정규화+탐지 처리에 500ms 타임아웃 적용. 초과 시 UNCERTAIN 처리 후 큐 적재 |
| 정규식 정적 분석 | CI에 `vulnregex-detector` 또는 RE2 호환 검사 추가 (선택적) |
| 커스텀 룰 패턴 검증 | 사용자가 등록하는 커스텀 룰 패턴은 저장 전 안전성 사전 검사 (테스트 문자열 세트로 실행 시간 측정) |

### 잔여 위험
안전성 검사를 통과한 패턴도 특이 입력 조합에서 느릴 수 있다. 운영 중 처리 시간 p99 모니터링으로 이상 탐지.

---

## T-05. 중복 삭제 (Double Delete / Idempotency Violation)

### 위협
동일한 댓글 삭제 요청이 두 번 이상 실행되어 Instagram API가 이미 삭제된 댓글에 DELETE를 재시도하는 오류가 발생한다. Worker 재시작, Webhook 중복 이벤트, 클라이언트 재시도가 원인이 될 수 있다.

### 공격 시나리오 (비악의적, 운영 시나리오)

1. Worker가 DELETE 성공 후 DB 상태 업데이트 전 크래시
2. 재시작 후 `status=PENDING` 상태인 Action을 재처리
3. Instagram API에 DELETE 재호출 → OAuthException 또는 404 수신
4. 오류 핸들링 미비 시 FAILED 상태로 남거나 무한 재시도 루프 진입

### 대응책

| 대응 | 구현 위치 |
|---|---|
| Idempotency Key | `moderation_actions.idempotency_key` (UNIQUE 제약). Worker 처리 전 키 존재 확인 |
| 멱등성 처리 | `idempotency_key` 충돌 시 기존 Action 상태 반환, 재처리 없음 |
| 이미 삭제된 댓글 처리 | Instagram `DELETE` 404/OAuthException → `status=ALREADY_PROCESSED`로 정상 종료. FAILED 처리 금지 |
| Webhook 중복 방어 | Redis에 `ig_comment_id` TTL 24h 저장. 중복 이벤트 silent drop |
| 상태 기계 전환 검증 | `COMPLETED` 또는 `ALREADY_PROCESSED` 상태에서 재처리 시도 차단 |

### 잔여 위험
TTL 24h 이후 동일 `ig_comment_id`가 재수신되면 중복 처리될 수 있다. 실제 Instagram 댓글 ID는 전역 유니크하므로 실제 충돌 확률은 극히 낮음.

---

## T-06. Audit Log 변조

### 위협
공격자 또는 내부자가 감사 로그를 수정·삭제해 악의적 행동(정상 댓글 대량 삭제, 룰 무단 변경 등)의 증거를 지운다.

### 공격 시나리오

1. DB 직접 접근 권한을 가진 내부자가 `audit_logs` 테이블의 행을 UPDATE/DELETE
2. 애플리케이션 레이어에서 감사 로그 기록 실패 시 비즈니스 트랜잭션만 커밋되어 추적 누락
3. 비즈니스 트랜잭션 롤백 시 같은 트랜잭션에서 기록한 감사 로그도 함께 롤백

### 대응책

| 대응 | 구현 위치 |
|---|---|
| INSERT ONLY 설계 | `audit_logs` 테이블에 UPDATE/DELETE 쿼리 경로 없음. 서비스 코드에 해당 쿼리 미구현 |
| DB 권한 분리 | 애플리케이션 DB 계정에 `audit_logs` 테이블 UPDATE/DELETE 권한 미부여 (DDL 분리) |
| 독립 트랜잭션 | 감사 로그는 비즈니스 트랜잭션과 분리된 별도 트랜잭션(`REQUIRES_NEW`)으로 기록. 비즈니스 롤백이 감사 로그를 삭제하지 않음 |
| 감사 로그 기록 실패 처리 | 감사 로그 저장 실패 시 비즈니스 로직도 실패 처리 (로그 누락보다 요청 실패가 안전) |
| 로그 보존 정책 강제 | 보존 기간 이전 레코드는 애플리케이션에서 조회 불가. 물리 삭제는 별도 승인 배치로만 수행 |

### 잔여 위험
DB 관리자 권한을 가진 AWS 루트 계정이 직접 삭제하는 경우 애플리케이션 레이어에서 방어 불가. AWS CloudTrail + RDS 감사 로그(pgaudit)로 DB 직접 접근 이력 모니터링.

---

## 위협 요약

| ID | 위협 | STRIDE | 영향 | 구현 우선순위 |
|---|---|---|---|---|
| T-01 | OAuth 토큰 탈취 | Information Disclosure | 치명적 | P0 (암호화 저장) |
| T-02 | Webhook 위조 | Spoofing / Tampering | 높음 | P0 (서명 검증) |
| T-03 | IDOR | Elevation of Privilege | 높음 | P0 (소유권 검증) |
| T-04 | ReDoS | Denial of Service | 중간 | P0 (입력 제한 + 타임아웃) |
| T-05 | 중복 삭제 | Tampering | 중간 | P0 (Idempotency Key) |
| T-06 | Audit Log 변조 | Tampering / Repudiation | 높음 | P0 (INSERT ONLY + 독립 트랜잭션) |
