# API Contract: K-SpamGuard
**버전 0.2 · 2026-05-05 · Base URL: `https://api.k-spamguard.io`**

---

## 공통 사항

### 인증
모든 엔드포인트는 JWT Bearer 토큰 필요 (단, Demo Comments와 Webhook 검증 챌린지 제외).

```
Authorization: Bearer {jwt_token}
```

### 외부 식별자 정책
- 응답에서 내부 PK(Long) 미노출. 모든 리소스는 `external_id`(UUID) 사용.
- 경로 파라미터 `{id}`는 항상 UUID.

### 공통 에러 형식

```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Comment not found",
    "timestamp": "2026-05-05T10:00:00Z"
  }
}
```

| HTTP 상태 | 에러 코드 | 설명 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 요청 파라미터/바디 형식 오류 |
| 401 | `UNAUTHORIZED` | JWT 미포함 또는 만료 |
| 403 | `FORBIDDEN` | 소유권 검증 실패 (타 사용자 리소스 접근) |
| 404 | `RESOURCE_NOT_FOUND` | 리소스 없음 |
| 409 | `DUPLICATE_ACTION` | 이미 처리된 Moderation Action |
| 429 | `RATE_LIMIT_EXCEEDED` | API Rate Limit 초과 (Bucket4j) |
| 503 | `UPSTREAM_UNAVAILABLE` | Instagram API Circuit Breaker OPEN 상태 |

---

## 1. Demo Comments API

포트폴리오 데모용 댓글 일괄 생성 및 즉시 탐지. 실제 Instagram 계정 없이 탐지 엔진 동작을 시연한다.

### `POST /api/v1/demo/comments`

**인증**: 불필요 (데모 전용, Rate Limit: 10 req/분)

**Request Body**

```json
{
  "comments": [
    {
      "external_comment_id": "demo_17858001234567890",
      "username": "spammer123",
      "text": "카·지·노 무료머니 지급 카카오 ID: abc123"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `comments` | array | Y | 댓글 목록 (최대 100개) |
| `comments[].external_comment_id` | string | Y | Instagram 댓글 ID (또는 임의 데모 ID) |
| `comments[].username` | string | Y | 작성자 계정명 |
| `comments[].text` | string | Y | 댓글 원문 |

**Response `201 Created`**

```json
{
  "imported_count": 3,
  "results": [
    {
      "external_comment_id": "demo_17858001234567890",
      "status": "SPAM",
      "score": 92,
      "reason_codes": ["GAMBLING_KEYWORD", "CONTACT_ID_PATTERN"]
    },
    {
      "external_comment_id": "demo_17858001234567891",
      "status": "SAFE",
      "score": 5,
      "reason_codes": []
    },
    {
      "external_comment_id": "demo_17858001234567892",
      "status": "SUSPECT",
      "score": 45,
      "reason_codes": ["PROMOTIONAL_PATTERN"]
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `imported_count` | int | 처리된 댓글 수 |
| `results[].status` | string | `SAFE` \| `SUSPECT` \| `SPAM` |
| `results[].score` | int | 스팸 점수 (0 – 100) |
| `results[].reason_codes` | string[] | 매칭된 룰 코드 목록 |

---

## 2. Detection API

### 2.1 댓글 목록 조회

**`GET /api/v1/accounts/{accountId}/comments`**

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `status` | string | (전체) | `SPAM` \| `SUSPECT` \| `SAFE` \| `PENDING` |
| `score_min` | float | 0.0 | 스팸 스코어 하한 (0.0 ~ 1.0) |
| `score_max` | float | 1.0 | 스팸 스코어 상한 |
| `from` | ISO 8601 | (7일 전) | 수신 시각 범위 시작 |
| `to` | ISO 8601 | (현재) | 수신 시각 범위 종료 |
| `cursor` | string | (없음) | 커서 기반 페이지네이션 |
| `limit` | int | 20 | 최대 100 |

**Response `200 OK`**

```json
{
  "data": [
    {
      "id": "uuid-...",
      "ig_comment_id": "17858001234567890",
      "raw_text": "카·지·노 무료머니 지급",
      "normalized_text": "카지노 무료머니 지급",
      "spam_score": 0.92,
      "status": "SPAM",
      "matched_rules": [
        {
          "id": "uuid-rule-...",
          "name": "GAMBLING_KEYWORD",
          "category": "GAMBLING",
          "weight": 0.6
        }
      ],
      "received_at": "2026-05-05T09:00:00Z"
    }
  ],
  "pagination": {
    "next_cursor": "eyJpZCI6MTIzfQ==",
    "has_more": true,
    "total_count": 152
  }
}
```

---

### 2.2 댓글 상세 조회

**`GET /api/v1/accounts/{accountId}/comments/{commentId}`**

**Response `200 OK`**

```json
{
  "id": "uuid-...",
  "ig_comment_id": "17858001234567890",
  "raw_text": "카·지·노 무료머니 지급 카카오 ID: abc123",
  "normalized_text": "카지노 무료머니 지급 카카오 ID abc123",
  "spam_score": 0.92,
  "status": "SPAM",
  "normalization_steps": [
    { "step": "NFKC", "before": "카·지·노", "after": "카지노" },
    { "step": "SPECIAL_CHAR_REMOVE", "before": "카·지·노", "after": "카지노" }
  ],
  "matched_rules": [
    {
      "id": "uuid-rule-...",
      "name": "GAMBLING_KEYWORD",
      "category": "GAMBLING",
      "weight": 0.6,
      "matched_pattern": "카지노"
    },
    {
      "id": "uuid-rule-...",
      "name": "CONTACT_ID_PATTERN",
      "category": "PHISHING",
      "weight": 0.32,
      "matched_pattern": "카카오 ID:"
    }
  ],
  "moderation_action_id": "uuid-action-...",
  "received_at": "2026-05-05T09:00:00Z"
}
```

---

### 2.3 댓글 재분석

룰 업데이트 후 이전 댓글을 다시 평가할 때 사용한다. 재분석 결과를 새 리소스로 생성한다.

**`POST /api/v1/accounts/{accountId}/comments/{commentId}/analyses`**

**Request Body**: 없음

**Response `201 Created`**

```json
{
  "id": "uuid-analysis-...",
  "comment_id": "uuid-...",
  "previous_status": "SAFE",
  "previous_score": 0.10,
  "new_status": "SPAM",
  "new_score": 0.85,
  "analyzed_at": "2026-05-05T10:30:00Z"
}
```

---

## 3. Moderation Queue API

### 3.1 검수 큐 목록

**`GET /api/v1/accounts/{accountId}/moderation`**

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `status` | string | `PENDING_REVIEW` | `PENDING_REVIEW` \| `APPROVED` \| `REJECTED` \| `COMPLETED` \| `FAILED` |
| `action_type` | string | (전체) | `HIDE` \| `DELETE` \| `UNHIDE` |
| `score_min` | float | 0.0 | 스팸 스코어 하한 |
| `from` | ISO 8601 | (7일 전) | 생성 시각 범위 시작 |
| `to` | ISO 8601 | (현재) | 생성 시각 범위 종료 |
| `cursor` | string | (없음) | 커서 페이지네이션 |
| `limit` | int | 20 | 최대 100 |

**Response `200 OK`**

```json
{
  "data": [
    {
      "id": "uuid-action-...",
      "comment": {
        "id": "uuid-comment-...",
        "ig_comment_id": "17858001234567890",
        "raw_text": "대.출 무직자 당일승인 카카오 ID: loan123",
        "normalized_text": "대출 무직자 당일승인 카카오 ID loan123",
        "spam_score": 0.87,
        "matched_rules": ["LOAN_KEYWORD", "CONTACT_ID_PATTERN"]
      },
      "status": "PENDING_REVIEW",
      "action_type": null,
      "idempotency_key": "uuid-idem-...",
      "created_at": "2026-05-05T09:05:00Z",
      "completed_at": null
    }
  ],
  "pagination": {
    "next_cursor": "eyJpZCI6NDU2fQ==",
    "has_more": false,
    "total_count": 8
  }
}
```

---

### 3.2 검수 항목 상태 변경 (승인 / 거부)

**`PATCH /api/v1/accounts/{accountId}/moderation/{actionId}`**

**소유권 검증**: `moderation.account.owner_id == current_user_id` — 불일치 시 403

**구독 한도 검증**: 월 처리 한도 초과 시 429 반환 (승인 시에만)

**Request Body — 승인**

```json
{
  "status": "APPROVED",
  "action_type": "HIDE"
}
```

**Request Body — 거부**

```json
{
  "status": "REJECTED"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | string | Y | `APPROVED` \| `REJECTED` |
| `action_type` | string | 조건부 | `HIDE` \| `DELETE` — `status: APPROVED` 시 필수 |

**Response `202 Accepted`** (승인) / **`200 OK`** (거부)

```json
{
  "id": "uuid-action-...",
  "status": "APPROVED",
  "action_type": "HIDE",
  "updated_at": "2026-05-05T10:00:00Z"
}
```

**에러**

| 코드 | 상태 | 설명 |
|---|---|---|
| `DUPLICATE_ACTION` | 409 | 이미 승인/거부된 항목 |
| `USAGE_LIMIT_EXCEEDED` | 429 | 플랜 월 처리 한도 초과 |
| `UPSTREAM_UNAVAILABLE` | 503 | Instagram API Circuit Breaker OPEN |

---

### 3.3 일괄 상태 변경

한 번에 최대 50개 처리 가능. 초과 시 `VALIDATION_ERROR`.

**`PATCH /api/v1/accounts/{accountId}/moderation`**

**Request Body**

```json
{
  "action_ids": ["uuid-action-1", "uuid-action-2"],
  "status": "APPROVED",
  "action_type": "HIDE"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `action_ids` | UUID[] | Y | 대상 Action ID 목록 (최대 50개) |
| `status` | string | Y | `APPROVED` \| `REJECTED` |
| `action_type` | string | 조건부 | `HIDE` \| `DELETE` — `status: APPROVED` 시 필수 |

**Response `202 Accepted`**

```json
{
  "accepted_count": 2,
  "rejected_count": 0,
  "results": [
    { "id": "uuid-action-1", "status": "APPROVED" },
    { "id": "uuid-action-2", "status": "APPROVED" }
  ]
}
```

부분 실패 시 해당 항목은 `"status": "ALREADY_PROCESSED"` 로 반환하고 나머지는 정상 처리.

---

### 3.4 숨김 복구 (오탐 복구)

자동 모드에서 잘못 숨김 처리된 댓글을 복구하는 새 액션을 생성한다.

**`POST /api/v1/accounts/{accountId}/moderation`**

**조건**: 원본 액션의 `status == COMPLETED && action_type == HIDE`

**Request Body**

```json
{
  "comment_id": "uuid-comment-...",
  "action_type": "UNHIDE"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `comment_id` | UUID | Y | 복구 대상 댓글 UUID |
| `action_type` | string | Y | `UNHIDE` 고정 |

**Response `202 Accepted`**

```json
{
  "id": "uuid-restore-action-...",
  "comment_id": "uuid-comment-...",
  "status": "APPROVED",
  "action_type": "UNHIDE",
  "created_at": "2026-05-05T10:10:00Z"
}
```

---

## 4. Audit Log API

### 4.1 감사 로그 목록

**`GET /api/v1/audit-logs`**

감사 로그는 사용자 스코프 전체 (모든 연동 계정 포함).

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `event_type` | string | (전체) | `login_success` \| `login_failure` \| `account_connect` \| `account_disconnect` \| `rule_create` \| `rule_update` \| `rule_delete` \| `moderation_approve` \| `moderation_reject` \| `token_refresh` \| `plan_change` |
| `account_id` | UUID | (전체) | 특정 Instagram 계정 필터 |
| `from` | ISO 8601 | (30일 전) | 이벤트 발생 시각 시작 |
| `to` | ISO 8601 | (현재) | 이벤트 발생 시각 종료 |
| `cursor` | string | (없음) | 커서 기반 페이지네이션 |
| `limit` | int | 50 | 최대 200 |

**Response `200 OK`**

```json
{
  "data": [
    {
      "id": "uuid-log-...",
      "event_type": "moderation_approve",
      "user_id": "uuid-user-...",
      "account_id": "uuid-account-...",
      "resource_id": "uuid-action-...",
      "before_value": null,
      "after_value": {
        "action_type": "HIDE",
        "comment_id": "uuid-comment-..."
      },
      "ip_address": "203.0.113.42",
      "created_at": "2026-05-05T10:00:00Z"
    },
    {
      "id": "uuid-log-2",
      "event_type": "rule_update",
      "user_id": "uuid-user-...",
      "account_id": "uuid-account-...",
      "resource_id": "uuid-rule-...",
      "before_value": { "pattern": "도박", "weight": 0.5, "is_active": true },
      "after_value":  { "pattern": "도박", "weight": 0.7, "is_active": true },
      "ip_address": "203.0.113.42",
      "created_at": "2026-05-05T09:50:00Z"
    }
  ],
  "pagination": {
    "next_cursor": "eyJpZCI6Nzg5fQ==",
    "has_more": true,
    "total_count": 347
  }
}
```

> **보존 기간 초과 레코드**: Free 30일 / Starter 90일 / Pro 1년. 범위 초과 조회 시 빈 결과 반환.

---

### 4.2 감사 로그 상세

**`GET /api/v1/audit-logs/{logId}`**

**Response `200 OK`**

```json
{
  "id": "uuid-log-...",
  "event_type": "rule_delete",
  "user_id": "uuid-user-...",
  "account_id": "uuid-account-...",
  "resource_id": "uuid-rule-...",
  "before_value": {
    "name": "대출키워드",
    "pattern": "대출|급전|신용불량",
    "weight": 0.6,
    "category": "LOAN",
    "is_active": true
  },
  "after_value": null,
  "ip_address": "203.0.113.42",
  "user_agent": "Mozilla/5.0 ...",
  "created_at": "2026-05-05T09:00:00Z"
}
```

**에러**

| 코드 | 상태 | 설명 |
|---|---|---|
| `RESOURCE_NOT_FOUND` | 404 | 해당 로그 없음 또는 보존 기간 만료 |
| `FORBIDDEN` | 403 | 다른 사용자의 감사 로그 접근 |
