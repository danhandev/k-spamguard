# Meta Instagram Graph API 기술 검증 (Spike)
**버전 0.1 · 2026-05-05 · API 버전: v25.0**
**작성 기준: Meta 공식 개발자 문서 (developers.facebook.com)**

---

## TL;DR — 구현 전략

```
Phase 1 (포트폴리오 MVP)
  Mock Adapter로 전체 유스케이스 완성
  → App Review 없이 Hexagonal Architecture / 보안 / 테스트 전략 증명 가능

Phase 2 (Spike)
  개발 모드 + 테스트 계정 10개로 Real Adapter 검증
  → 실제 API 동작, 에러 코드, Rate Limit 확인

Phase 3 (SaaS 출시)
  Business Verification → App Review 통과 → Live 모드
  → 제3자 계정(실제 사용자) 온보딩 가능
```

> **핵심 제약**: App Review를 통과하기 전에는 실제 고객 계정을 연동할 수 없다.  
> 포트폴리오 MVP는 Mock Adapter로 완성하고, Real Adapter는 Spike 수준에서 붙이는 것이 안전하다.

---

## 1. API 경로 선택

Meta는 두 가지 연동 경로를 제공한다. **K-SpamGuard는 Instagram Login 경로를 채택한다.**

| 구분 | Instagram Login (`graph.instagram.com`) | Facebook Login (`graph.facebook.com`) |
|---|---|---|
| 계정 연결 | Instagram 계정 직접 OAuth | Facebook 페이지 연결 필수 |
| 지원 계정 | Business + Creator | Business (페이지 보유 계정) |
| 코어 스코프 | `instagram_business_basic` `instagram_business_manage_comments` | `instagram_basic` `instagram_manage_comments` `pages_read_engagement` |
| Webhook 추가 스코프 | 없음 | `pages_manage_metadata` `pages_show_list` |
| 토큰 만료 | 60일 (갱신 필요) | Page Access Token — 만료 없음 (시스템 유저 기준) |
| **채택 이유** | ✅ Creator 계정 지원, 페이지 불필요 | ❌ Facebook 페이지 없는 크리에이터 연동 불가 |

> **주의**: 구 스코프명 (`business_basic`, `instagram_manage_comments` without `instagram_business_` prefix)은  
> **2025년 1월 27일부로 deprecated**. 반드시 `instagram_business_` 접두어 버전 사용.

---

## 2. 필요한 계정 유형

| 조건 | 내용 |
|---|---|
| 지원 유형 | **Instagram 비즈니스 계정** 또는 **크리에이터 계정** |
| 미지원 유형 | 일반 개인 계정 (Personal Account) — API 접근 불가 |
| 사용자 안내 | 연동 전 계정 유형 전환 가이드 제공 필요 |

**온보딩 체크**: OAuth 완료 후 `GET /me?fields=account_type` 으로 계정 유형 확인.  
`PERSONAL` 반환 시 연동 거부 + 전환 가이드 링크 표시.

---

## 3. 필요한 권한 (Instagram Login 경로)

| 권한 | 용도 | Access Level |
|---|---|---|
| `instagram_business_basic` | 계정 정보 조회, 미디어 목록 | Standard Access |
| `instagram_business_manage_comments` | 댓글 읽기·숨김·삭제 | **Advanced Access** |

> **Advanced Access = App Review 필수**.  
> SaaS로서 제3자 계정을 다루려면 Advanced Access 승인이 반드시 선행되어야 한다.

---

## 4. App Review 필요 여부

### 4.1 개발 모드 (App Review 불필요)
- 앱 대시보드에 **역할(Role)** 이 부여된 사용자만 사용 가능
- 역할 유형: Admin, Developer, Tester
- **테스트 계정 한도: 최대 10개** (하드 캡)
- 모든 권한이 역할 사용자에게는 App Review 없이 활성화

→ **포트폴리오 데모, Spike 검증, 내부 테스트는 개발 모드로 충분**

### 4.2 Live 모드 (App Review 필요)

실제 고객(제3자 계정)이 앱을 OAuth 연동하려면:

1. **Business Verification 완료** (Meta에 사업자 정보 제출, 선행 필수)
2. **App Review 신청**:
   - 사용 목적 기술
   - 전체 OAuth 플로우 + 댓글 읽기/숨김/삭제 동작을 담은 스크린캐스트 제출
   - `instagram_business_manage_comments`에 대한 Advanced Access 요청
3. **심사 기간**: 수 주 소요 가능. 거절 시 재신청 가능.

### 4.3 MVP 구현 순서 (권장)

```
개발 모드 (테스트 10계정)
  → Spike 검증 완료
  → Business Verification 제출
  → App Review 신청
  → 승인 후 Live 모드 전환
  → 실제 사용자 온보딩
```

> **절대 하면 안 되는 것**: App Review 승인 전에 Live 모드로 전환하면,  
> 일반 사용자가 OAuth 진행 시 `instagram_business_manage_comments` 스코프 승인이 **무음으로 실패**한다.

---

## 5. 댓글 조회 API

```
GET https://graph.instagram.com/v25.0/{IG_MEDIA_ID}/comments
```

### 요청 파라미터

| 파라미터 | 설명 |
|---|---|
| `fields` | 반환 필드 지정. 미지정 시 `id`, `text`, `timestamp`만 반환 |
| `access_token` | Long-lived 토큰 |
| `after` | 커서 기반 페이지네이션 (다음 페이지) |
| `before` | 커서 기반 페이지네이션 (이전 페이지) |

### 사용 가능한 fields

```
id, text, timestamp, from(id, username), hidden,
like_count, media(id, product_type),
parent_id, replies{id,text,timestamp,from}, username, legacy_instagram_comment_id
```

### 응답 특성

| 항목 | 내용 |
|---|---|
| 정렬 | **최신순 (Reverse Chronological)** — v3.2 이후 |
| 페이지당 최대 | **50개** |
| 날짜 필터 | **미지원** — `timestamp` 파라미터 없음 |
| 답글 포함 여부 | 기본 미포함. `replies` 필드 요청 시 포함 |
| Rate Limit 에러 | HTTP 400, 에러 코드 **80002** |

### 구현 주의사항

```
- 초기 백필(Backfill): 계정 연동 시 이 API로 기존 댓글 일괄 수집
- 이후 실시간 수집: Webhook 사용 (폴링 금지 — Rate Limit 소진 위험)
- 폴링 백업(P1): Webhook 미수신 대비 1시간 주기 보완 수집
```

---

## 6. 댓글 숨김 API

```
POST https://graph.instagram.com/v25.0/{IG_COMMENT_ID}
Content-Type: application/x-www-form-urlencoded

hide=true&access_token={TOKEN}
```

### 응답

```json
{ "success": true }
```

### 숨김 해제 (복구)

```
POST /{IG_COMMENT_ID}?hide=false&access_token={TOKEN}
```

### 특이사항

| 항목 | 내용 |
|---|---|
| 복구 가능 여부 | ✅ **가능** — `hide=false`로 원복 |
| 멱등성 | 공식 보장 없음. 이미 숨겨진 댓글에 `hide=true` → `{"success":true}` 반환 (실험적 확인) |
| 계정 소유자 댓글 | ❌ **숨김 불가** — 미디어 소유자가 자신의 댓글에 `hide=true` 불가 |
| 라이브 방송 댓글 | ❌ 미지원 |
| 답글 | 답글(`parent_id` 있는 댓글) 숨김 가능 여부 미문서화 — Spike에서 검증 필요 |

---

## 7. 댓글 삭제 API

```
DELETE https://graph.instagram.com/v25.0/{IG_COMMENT_ID}?access_token={TOKEN}
```

### 응답

```json
{ "success": true }
```

### 특이사항

| 항목 | 내용 |
|---|---|
| 복구 가능 여부 | ❌ **영구 삭제** — 복구 불가 |
| 삭제 권한 | 미디어 소유자가 자신의 미디어에 달린 댓글 삭제 가능 |
| 이미 삭제된 댓글 재삭제 | OAuthException 또는 404 반환 — 에러 핸들링 필수 |
| 라이브 방송 댓글 | ❌ 미지원 |

> **K-SpamGuard 전략**: 삭제는 숨김 이후 명시적 선택 시에만 호출.  
> 자동 모드에서는 기본 액션을 `hide`로 제한. `delete`는 사용자가 직접 선택한 경우만.

---

## 8. Webhook 구성

### 8.1 구독 등록

```
POST /me/subscribed_apps?subscribed_fields=comments&access_token={TOKEN}
```

### 8.2 검증 챌린지 (최초 등록 시)

Meta가 GET 요청 전송:
```
GET {YOUR_WEBHOOK_URL}
  ?hub.mode=subscribe
  &hub.challenge=1158201444
  &hub.verify_token={YOUR_VERIFY_TOKEN}
```

응답 조건:
- `hub.verify_token`이 사전에 설정한 값과 일치하는지 검증
- `hub.challenge` 값을 **그대로** plain text로 HTTP 200 응답
- 불일치 또는 비200 응답 → 구독 등록 실패

### 8.3 이벤트 페이로드 구조

```json
{
  "object": "user",
  "entry": [
    {
      "id": "{IG_USER_ID}",
      "time": 1520383571,
      "changes": [
        {
          "field": "comments",
          "value": {
            "verb": "update",
            "object_id": "{IG_COMMENT_ID}",
            "from": {
              "id": "{SCOPED_USER_ID}",
              "username": "{COMMENTER_USERNAME}"
            },
            "text": "댓글 원문",
            "parent_id": "{PARENT_COMMENT_ID}",
            "media": {
              "id": "{IG_MEDIA_ID}",
              "product_type": "FEED"
            }
          }
        }
      ]
    }
  ]
}
```

### 8.4 서명 검증

```
X-Hub-Signature-256: sha256={HMAC_SHA256(APP_SECRET, REQUEST_BODY)}
```

구현:
```java
// WebhookController.java
String signature = request.getHeader("X-Hub-Signature-256");
String expected = "sha256=" + HmacSHA256(appSecret, rawBody);
if (!MessageDigest.isEqual(signature.getBytes(), expected.getBytes())) {
    return ResponseEntity.badRequest().build(); // 400
}
```

> **주의 — mTLS 인증서 업데이트 (이미 경과)**:  
> Meta는 2026년 3월 31일부로 새 CA 인증서(`meta-outbound-api-ca-2025-12.pem`)로 전환했다.  
> 현재(2026-05-05)는 이미 전환 완료 시점. Webhook 서버의 TLS trust store에  
> 해당 CA 인증서가 없으면 현재 Webhook 수신이 실패하고 있을 수 있음.  
> **Spike 전 서버 trust store 확인 필수.**

### 8.5 Webhook 구독 주의사항

| 항목 | 내용 |
|---|---|
| `live_comments` 구독 | 라이브 방송 댓글 — 트래픽 폭증 위험. **MVP에서 구독 금지** |
| 중복 이벤트 | Instagram은 동일 이벤트를 최대 3회 재발송 가능 → **idempotency_key 필수** |
| 앨범/카루셀 미디어 ID | Webhook 페이로드에 포함되지 않음 → 댓글 ID로 미디어 ID 별도 조회 필요 |

---

## 9. Access Token 저장 및 갱신 전략

### 9.1 토큰 생명주기

```
OAuth 완료
  → Short-lived Token (유효기간 1시간)
  → GET /access_token?grant_type=ig_exchange_token (교환)
  → Long-lived Token (유효기간 60일)
  → GET /refresh_access_token?grant_type=ig_refresh_token (갱신)
  → 새 Long-lived Token (60일 리셋)
```

### 9.2 갱신 조건

- 토큰 발급 후 **24시간 이상 경과** (그 전에는 갱신 API 거부)
- 토큰이 아직 **유효한 상태** (만료 후 갱신 불가 — 재인증 필요)
- **60일 미사용 시 영구 만료** — 갱신 불가, 사용자가 다시 OAuth 진행해야 함

### 9.3 K-SpamGuard 갱신 전략

```
배치 스케줄러 (매일 1회 실행)
  1. 만료까지 10일 이하인 토큰 조회
  2. Refresh API 호출
  3. 새 토큰 AES-256-GCM 암호화 → DB 업데이트
  4. 실패 시 → 계정 소유자에게 재인증 이메일 발송

만료 30일 전: 대시보드 배너 경고
만료 7일 전:  이메일 알림
만료 후:      계정 상태 = TOKEN_EXPIRED, 모든 액션 차단, 재연동 유도
```

### 9.4 토큰 보안 저장

```java
// AesGcmTokenEncryptor.java
// 암호화 (저장 시)
byte[] iv = generateRandomIv(); // 12 bytes, per-token
byte[] encrypted = aesGcm.encrypt(rawToken, encryptionKey, iv);
// DB 저장: encrypted_token, token_iv (분리 저장)

// 복호화 (사용 시)
String rawToken = aesGcm.decrypt(encryptedToken, encryptionKey, iv);
// 복호화된 토큰은 메모리에만 존재, 로그에 절대 기록 금지
```

---

## 10. Rate Limit 대응

### 10.1 Rate Limit 계산식

```
24시간 허용 호출 수 = 4,800 × (최근 24시간 노출 수)
```

- **노출 수**가 낮은 소규모 계정은 허용 호출 수도 낮음
- 대형 계정(수백만 노출)은 수백만 호출 가능

### 10.2 Rate Limit 감지

응답 헤더 모니터링:
```
X-Business-Use-Case-Usage: {
  "{IG_USER_ID}": [{
    "call_count": 28,       // 현재 사용률 %
    "total_cputime": 25,    // CPU 사용률 %
    "total_time": 25,       // 총 시간 사용률 %
    "type": "messenger",
    "estimated_time_to_regain_access": 0
  }]
}
```

에러 코드: HTTP 400, `"code": 80002`

### 10.3 K-SpamGuard 대응 전략

```
읽기 작업
  - Webhook 우선 (Rate Limit 미소모)
  - 폴링은 오직 Webhook 누락 보완용 (1시간 주기, P1)

쓰기 작업 (숨김/삭제)
  - X-Business-Use-Case-Usage.call_count > 80% → 액션 지연 처리 (큐 적재, 다음 시간대 실행)
  - 에러 80002 수신 → Exponential Backoff (1s, 2s, 4s, 최대 3회)
  - Circuit Breaker: 연속 5회 실패 → 30초 OPEN

배치 처리
  - Graph API Batch Endpoint (`POST /`) 사용 검토 — 단일 HTTP 요청에 여러 API 묶기
```

---

## 11. 개발 모드 vs 운영 모드 차이

| 구분 | 개발 모드 | 운영(Live) 모드 |
|---|---|---|
| 접근 가능 사용자 | 앱 역할 부여된 사용자만 | 모든 Instagram 사용자 |
| App Review | 불필요 | **Advanced Access 필수** |
| 테스트 계정 한도 | **최대 10개** | 무제한 |
| 권한 사용 | 모든 권한 즉시 사용 가능 | 승인된 권한만 |
| 개발 중 데이터 | Live 전환 시 공개 전환 주의 | 공개 |
| **주의 사항** | 10개 초과 베타는 불가 | App Review 전 전환 금지 |

---

## 12. 실패 가능성이 높은 지점

| # | 실패 지점 | 원인 | 대응 |
|---|---|---|---|
| F-01 | **Webhook 수신 실패** | mTLS 인증서 미갱신 (2026-03-31 deadline 경과) | Spike 전 서버 trust store에 `meta-outbound-api-ca-2025-12.pem` 추가 여부 확인 |
| F-02 | **OAuth 스코프 승인 거부** | Live 모드인데 App Review 미완료 | 개발 모드에서만 테스트. Live 전환은 승인 후 |
| F-03 | **토큰 만료 인지 실패** | 60일 갱신 누락 | 배치 갱신 스케줄러 + 만료 경고 알림 |
| F-04 | **계정 소유자 댓글 숨김 시도** | 소유자 댓글은 hide 불가 | API 호출 전 `from.id == ig_account_id` 체크 → skip |
| F-05 | **이미 삭제된 댓글 재삭제** | Webhook 중복 + 이미 수동 삭제된 경우 | `OAuthException` / 404 → ModerationAction status = ALREADY_PROCESSED |
| F-06 | **Webhook 중복 이벤트** | Instagram 최대 3회 재발송 | Redis idempotency key (`ig_comment_id`, TTL 24h) |
| F-07 | **Rate Limit 초과** | 소규모 계정의 낮은 허용 한도 + 스팸 폭주 | 헤더 모니터링 + 80% 도달 시 큐 지연 |
| F-08 | **구 스코프명 사용** | `instagram_manage_comments` (deprecated) | `instagram_business_manage_comments` 확인 |
| F-09 | **앱 심사 거절** | 스크린캐스트 미흡, 사용 목적 불명확 | 심사 전 가이드 숙지, 전체 플로우 영상 준비 |
| F-10 | **Live 모드 전환 후 일반 사용자 연동 실패** | App Review 전 전환 | 승인 확인 후 전환 |

---

## 13. 테스트 계정 검증 체크리스트

개발 모드에서 테스트 계정 (최대 10개)으로 검증할 항목.

### 13.1 OAuth & 계정 연동

```
[ ] 비즈니스 계정 OAuth → 토큰 수신 → Long-lived 교환 성공
[ ] 크리에이터 계정 OAuth → 동일 플로우 성공
[ ] 일반 개인 계정 OAuth → account_type 확인 후 연동 거부 메시지 표시
[ ] 토큰 AES-256-GCM 암호화 저장 → 복호화 후 API 호출 성공
[ ] Audit Log: account_connect 기록 확인
```

### 13.2 Webhook

```
[ ] 검증 챌린지(hub.challenge) 응답 → 구독 등록 성공
[ ] X-Hub-Signature-256 서명 검증 → 유효 요청 통과
[ ] 서명 불일치 → 400 반환 확인
[ ] 댓글 작성 이벤트 → Webhook 수신 → DB 저장 확인
[ ] 동일 이벤트 3회 재발송 → 1회만 처리 (idempotency 검증)
[ ] Webhook 페이로드에서 media_id 추출 성공
```

### 13.3 댓글 조회 API

```
[ ] GET /{media-id}/comments → 댓글 목록 반환
[ ] fields 지정 → from, hidden, timestamp 포함 반환
[ ] 페이지네이션 cursor → 50개 초과 댓글 전체 수집
[ ] hidden 필드 → 숨겨진 댓글도 미디어 소유자에게 반환되는지 확인
```

### 13.4 숨김 API

```
[ ] POST /{comment-id}?hide=true → {"success":true} 반환
[ ] GET /{media-id}/comments → 숨겨진 댓글 hidden=true 확인
[ ] 다른 사용자 계정으로 접근 → 해당 댓글 미노출 확인
[ ] POST /{comment-id}?hide=false → 숨김 해제 성공
[ ] 이미 숨겨진 댓글에 hide=true 재호출 → 응답 확인 (멱등성)
[ ] 미디어 소유자 자신의 댓글 hide=true → 에러 코드 기록
```

### 13.5 삭제 API

```
[ ] DELETE /{comment-id} → {"success":true} 반환
[ ] 삭제 후 GET /{media-id}/comments → 해당 댓글 미반환 확인
[ ] 이미 삭제된 comment-id 재호출 → 에러 코드 기록 (OAuthException 또는 404)
[ ] Audit Log: moderation_approve (delete) 기록 확인
```

### 13.6 Rate Limit

```
[ ] X-Business-Use-Case-Usage 헤더 수신 확인
[ ] call_count 값 파싱 → 80% 초과 시 큐 지연 동작 검증
[ ] 80002 에러 재현 (가능한 경우) → Backoff 동작 확인
```

### 13.7 토큰 갱신

```
[ ] Long-lived 토큰 발급 후 24시간 후 갱신 API 호출 → 성공
[ ] 24시간 미경과 갱신 → 에러 코드 기록
[ ] 갱신 후 토큰으로 API 호출 → 정상 동작 확인
```

---

## 14. Spike 구현 목표

Spike는 "동작하는 Real Adapter"가 목표가 아니라 "가정 검증"이 목표다.

| 검증 가정 | 확인 방법 |
|---|---|
| hide/delete API가 K-SpamGuard의 멱등성 패턴에서 안전하게 동작하는가 | 13.4, 13.5 체크리스트 |
| Webhook이 실제로 실시간으로 수신되는가 | 13.2 체크리스트 |
| Rate Limit이 소규모 계정에서 실제로 문제가 되는가 | 13.6 체크리스트 |
| 토큰 갱신 플로우가 60일 전 정상 동작하는가 | 13.7 체크리스트 |
| mTLS 이슈로 Webhook이 현재 막혀 있는가 | Spike 시작 전 즉시 확인 |

**Spike 완료 기준**: 체크리스트 전 항목 통과 + 에러 코드별 응답 형식 문서화.

---

## 15. 참고 링크

| 문서 | URL |
|---|---|
| Instagram Platform Overview | https://developers.facebook.com/docs/instagram-platform/overview/ |
| Comment Moderation Guide | https://developers.facebook.com/docs/instagram-platform/comment-moderation/ |
| IG Comment API Reference | https://developers.facebook.com/docs/instagram-platform/instagram-graph-api/reference/ig-comment/ |
| IG Media Comments | https://developers.facebook.com/docs/instagram-platform/instagram-graph-api/reference/ig-media/comments/ |
| Instagram Login Webhooks | https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/webhooks |
| Business Login Tokens | https://developers.facebook.com/docs/instagram-platform/instagram-api-with-instagram-login/business-login |
| Graph API Rate Limiting | https://developers.facebook.com/docs/graph-api/overview/rate-limiting/ |
| App Review Introduction | https://developers.facebook.com/docs/resp-plat-initiatives/app-review/introduction |
| Graph API v25.0 Changelog | https://developers.facebook.com/blog/post/2026/02/18/introducing-graph-api-v25-and-marketing-api-v25/ |
