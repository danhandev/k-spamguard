# Project: K-SpamGuard

## Goal
한국어 인스타그램 스팸 댓글 탐지 및 자동 관리 SaaS.
금융권 수준의 보안, 감사 로깅, 멱등성, 외부 API 신뢰성을 포트폴리오로 증명한다.

## Architecture Principles
- Hexagonal Architecture (Ports & Adapters): domain → application → infrastructure 방향으로만 의존
- 도메인 로직은 Spring, JPA, Redis에 의존하지 않는다
- Instagram API는 반드시 Port 인터페이스로 추상화하고 어댑터로 구현
- 모든 외부 API 호출은 Circuit Breaker(Resilience4j)와 재시도 정책을 적용
- 모든 상태 변경은 Audit Log를 남긴다

## Security Rules
- OAuth 액세스 토큰은 AES-256-GCM으로 암호화 후 저장, 복호화는 애플리케이션 레이어에서만
- 외부 응답에 내부 PK(Long) 노출 금지 — 외부 식별자는 UUID 사용
- 모든 리소스 접근 시 소유권(ownership) 검증 필수
- 공개 API에 Rate Limiting 적용 (Bucket4j)
- Audit Log 필수 항목: login, account_connect, account_disconnect, rule_create, rule_update, rule_delete, moderation_approve, moderation_reject
- 로그에 access_token, Authorization 헤더, secret 원문 절대 기록 금지
- Secret은 환경 변수 또는 AWS Secrets Manager로 관리, 코드/Git에 하드코딩 금지

## Data Rules
- 스키마 변경은 Flyway 마이그레이션으로만 관리
- 삭제는 soft delete (deleted_at) 원칙, 물리 삭제는 별도 배치
- 멱등성: Webhook 수신 및 외부 API 액션은 idempotency key로 중복 처리 방지
- 구독 플랜별 월 처리 한도(usage limit)를 초과하면 액션 차단

## Testing Rules
- 도메인 로직: 단위 테스트 (JUnit 5, AssertJ)
- Repository / 트랜잭션: Testcontainers + PostgreSQL 통합 테스트
- Instagram API: Mock 어댑터로 대체 (실제 API 호출 금지)
- 실패 케이스, 재시도, 멱등성 케이스 반드시 포함
- 서비스 레이어 행동 하나당 테스트 하나 이상

## Output Rules
코드 편집 전:
1. 구현 계획 설명
2. 변경할 파일 목록
3. 인수 조건(Acceptance Criteria) 정의

코드 편집 후:
1. 테스트 실행
2. 변경된 파일 요약
3. 남은 리스크 언급

## Commit Rules
- 커밋 메시지에 `Co-Authored-By: Claude` 트레일러 추가 금지
- 커밋 작성자는 항상 사용자(danhandev)로만 표시

## What NOT to Do
- 불필요한 추상화 계층 추가 금지
- 현재 요구사항에 없는 기능 선제 구현 금지
- 설명성 주석 금지 (WHY가 자명하지 않을 때만 작성)
- `git add -A` 금지 — 민감 파일 스테이징 방지
