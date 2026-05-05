# Architecture: K-SpamGuard

## 1. System Overview

```
Instagram ──Webhook──▶ [Spring Boot API] ──▶ PostgreSQL
                              │               Redis
                              │               (Queue / Cache)
                              ▼
                       [Moderation Worker]
                              │
                              ▼
                       Instagram Graph API
                       (hide / delete)

User ──HTTPS──▶ [Next.js Dashboard] ──API──▶ [Spring Boot API]
```

## 2. Component Breakdown

### 2.1 Backend (Spring Boot 3 / Java 21)

Hexagonal Architecture 적용.

```
com.kspamguard/
├── domain/                     # 순수 도메인 (외부 의존 없음)
│   ├── comment/
│   │   ├── Comment.java        # 댓글 엔티티
│   │   └── SpamResult.java     # 탐지 결과 값 객체
│   ├── rule/
│   │   ├── SpamRule.java       # 룰 엔티티
│   │   └── RuleSet.java        # 룰 집합
│   ├── moderation/
│   │   ├── ModerationAction.java
│   │   └── ModerationStatus.java
│   ├── account/
│   │   ├── InstagramAccount.java
│   │   └── OAuthToken.java     # 암호화된 토큰 값 객체
│   └── subscription/
│       ├── Plan.java
│       └── UsageCounter.java
│
├── application/                # 유스케이스, Port 인터페이스
│   ├── comment/
│   │   └── IngestCommentUseCase.java
│   ├── detection/
│   │   ├── NormalizationPipeline.java  # 정규화 단계 체인
│   │   └── RuleEngine.java             # 룰 평가기
│   ├── moderation/
│   │   ├── ApproveModerationUseCase.java
│   │   └── RejectModerationUseCase.java
│   ├── audit/
│   │   └── AuditLogService.java
│   └── port/                   # 외부 시스템 Port (인터페이스)
│       ├── out/
│       │   ├── InstagramCommentPort.java   # 숨김/삭제
│       │   ├── CommentRepository.java
│       │   ├── RuleRepository.java
│       │   ├── ModerationRepository.java
│       │   └── AuditLogRepository.java
│       └── in/
│           └── WebhookPort.java
│
├── infrastructure/             # 어댑터 구현체
│   ├── instagram/
│   │   ├── InstagramGraphApiAdapter.java   # 실제 API 어댑터
│   │   └── FakeInstagramAdapter.java       # 테스트용 Mock 어댑터
│   ├── persistence/
│   │   ├── CommentJpaRepository.java
│   │   └── entity/                         # JPA @Entity
│   ├── cache/
│   │   └── RedisIdempotencyStore.java
│   └── security/
│       └── AesGcmTokenEncryptor.java
│
└── presentation/
    ├── api/
    │   ├── CommentController.java
    │   ├── RuleController.java
    │   ├── ModerationController.java
    │   ├── AuditLogController.java
    │   └── AccountController.java
    └── webhook/
        └── InstagramWebhookController.java
```

### 2.2 Frontend (Next.js 14)

```
src/
├── app/
│   ├── (auth)/login/
│   ├── dashboard/
│   │   ├── page.tsx            # 통계 요약
│   │   ├── comments/           # 댓글 목록 + 탐지 결과
│   │   ├── moderation/         # Moderation Queue
│   │   ├── rules/              # 룰 관리
│   │   ├── audit/              # Audit Log 뷰어
│   │   └── settings/           # 계정 연동, 플랜
│   └── layout.tsx
├── components/
│   ├── moderation/
│   │   ├── ModerationQueue.tsx
│   │   └── CommentCard.tsx
│   ├── rules/
│   │   └── RuleEditor.tsx
│   └── audit/
│       └── AuditLogTable.tsx
└── lib/
    ├── api.ts                  # API 클라이언트
    └── auth.ts                 # NextAuth 설정
```

### 2.3 Infrastructure

```
infra/
├── docker/
│   ├── postgres/init.sql
│   └── redis/redis.conf
├── nginx/
│   └── nginx.conf              # TLS termination, reverse proxy
└── scripts/
    ├── seed-demo.sh            # 데모 데이터 적재
    └── rotate-token.sh         # OAuth 토큰 갱신
```

## 3. Data Flow

### 3.1 Webhook → 스팸 탐지

```
1. POST /webhook/instagram
   └─ X-Hub-Signature-256 검증 (HMAC-SHA256)
   └─ idempotency: ig_comment_id로 중복 확인 (Redis)
2. Comment 저장 (status=PENDING)
3. NormalizationPipeline 실행
   ├─ Unicode NFKC 정규화
   ├─ 특수문자 제거
   ├─ 숫자→문자 치환
   ├─ 초성 패턴 탐지
   └─ URL 추출
4. RuleEngine 평가
   └─ 각 SpamRule.weight 합산 → threshold 비교
5. SpamResult 저장 (SPAM / HAM / UNCERTAIN)
6. SPAM → ModerationAction 생성 (status=PENDING_REVIEW)
7. 자동 모드 활성 && score ≥ auto_threshold → 즉시 Queue 적재
```

### 3.2 Moderation 승인 → Instagram 액션

```
1. POST /moderation/{id}/approve
   └─ 소유권 검증: moderation.account.ownerId == currentUserId
   └─ 구독 한도 확인: usageCounter.check(plan)
2. ModerationAction status → APPROVED
3. Redis Queue에 액션 메시지 발행
4. Worker: Queue 소비
   ├─ InstagramCommentPort.hide(commentId) 또는 .delete(commentId)
   ├─ 실패 시 Exponential Backoff 재시도 (최대 3회)
   ├─ Circuit Breaker: 연속 5회 실패 시 30초 open
   └─ 최종 실패 시 ModerationAction status → FAILED
5. 성공 시 status → COMPLETED
6. AuditLog 기록: moderation_approve
```

## 4. Key Design Decisions

### 4.1 Hexagonal Architecture
**Why**: Instagram API 어댑터를 Mock으로 교체하면 통합 테스트에서 실제 API 호출 없이 전체 유스케이스를 검증할 수 있다. 금융권에서 흔히 요구하는 "외부 시스템 교체 가능성" 시연.

### 4.2 Idempotency Key (Redis)
**Why**: Instagram Webhook은 동일 이벤트를 중복 발송할 수 있다. Redis에 `ig_comment_id`를 TTL 24h로 저장해 중복 처리를 방지.

### 4.3 Async Moderation Queue (Redis List)
**Why**: Instagram API 응답 지연이 Webhook 처리를 막지 않도록 분리. 구독 한도 초과 시 Queue 적재 자체를 차단.

### 4.4 Token Encryption (AES-256-GCM)
**Why**: DB 탈취 시 OAuth 토큰 직접 노출 방지. 복호화 키는 환경 변수로만 관리.

### 4.5 Flyway Migrations
**Why**: 스키마 변경 이력 추적, 팀 환경 재현, 롤백 가능한 마이그레이션. 금융권 표준.

### 4.6 UUID as External ID
**Why**: 내부 PK(Long) 순서 노출 시 리소스 개수 유추 가능. 외부에는 UUID만 노출.

## 5. Database Schema (주요 테이블)

```sql
-- 사용자
users (id BIGSERIAL, external_id UUID, email, created_at, deleted_at)

-- Instagram 계정
instagram_accounts (id, external_id UUID, owner_id→users, ig_account_id,
                    encrypted_access_token, token_iv, plan_id, created_at, deleted_at)

-- 구독 사용량
usage_counters (id, account_id, year_month, action_count, limit_count)

-- 댓글
comments (id, external_id UUID, account_id, ig_comment_id UNIQUE,
          raw_text, normalized_text, status, spam_score, received_at)

-- 스팸 룰
spam_rules (id, external_id UUID, account_id, name, pattern, weight,
            category, is_active, created_at, updated_at, deleted_at)

-- Moderation 액션
moderation_actions (id, external_id UUID, account_id, comment_id,
                    action_type, status, idempotency_key UNIQUE,
                    retry_count, created_at, completed_at)

-- Audit Log (INSERT ONLY, 수정/삭제 금지)
audit_logs (id, user_id, account_id, event_type, resource_id,
            before_value JSONB, after_value JSONB, ip_address, created_at)
```

## 6. Security Architecture

| 항목 | 구현 |
|---|---|
| 인증 | JWT (Spring Security) |
| 권한 | 리소스별 소유권 검증 (Service Layer) |
| 토큰 암호화 | AES-256-GCM, IV per token |
| Rate Limiting | Bucket4j (API별 설정) |
| Webhook 서명 검증 | HMAC-SHA256 (X-Hub-Signature-256) |
| Secret 관리 | 환경 변수 / AWS Secrets Manager |
| 로그 마스킹 | Logback 패턴으로 token/Authorization 마스킹 |
| 감사 | audit_logs 테이블 (INSERT ONLY) |

## 7. Infrastructure (Production Target)

```
AWS
├── ECS Fargate: Spring Boot API (2 task)
├── ECS Fargate: Next.js Frontend (2 task)
├── RDS PostgreSQL (Multi-AZ)
├── ElastiCache Redis
├── ALB (HTTPS, 443)
├── ACM (TLS 인증서)
├── Secrets Manager (DB 비밀번호, 암호화 키, Instagram App Secret)
└── CloudWatch (로그, 메트릭, 알람)

GitHub Actions
├── test (PR): Gradle test (Testcontainers)
├── build (main): Docker 이미지 빌드 → ECR push
└── deploy (main): ECS 서비스 업데이트
```

## 8. Local Development

```bash
docker compose up -d        # PostgreSQL + Redis 기동
cd backend && ./gradlew bootRun  # API 서버 :8080
cd frontend && npm run dev       # 대시보드 :3000
```

`docker-compose.yml`에는 PostgreSQL, Redis만 포함. 앱은 로컬에서 실행.

## 9. External API Reliability

| 패턴 | 적용 위치 |
|---|---|
| Circuit Breaker | InstagramGraphApiAdapter (Resilience4j) |
| Retry (Exponential Backoff) | Moderation Worker |
| Timeout | Instagram API 호출 (5초) |
| Rate Limit 추적 | 계정별 시간당 액션 카운터 (Redis) |
| Idempotency | Moderation Action (idempotency_key 컬럼) |
