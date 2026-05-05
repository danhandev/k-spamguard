# K-SpamGuard

한국어 인스타그램 스팸 댓글 탐지 및 자동 관리 SaaS

## Problem
한국어 스팸 댓글은 특수문자 삽입, 초성 분리(ㅅㅍㅇ), 숫자 치환(0→o, 1→l), 이모지 혼입 등 변형 기법으로 단순 키워드 필터를 우회한다.
인스타그램 소상공인·크리에이터는 하루 수백 개의 스팸 댓글을 수동 관리해야 하며, 이는 브랜드 신뢰도 하락과 운영 비용 증가로 이어진다.

## Solution
댓글을 Webhook으로 수집하고, 한국어 정규화 파이프라인과 룰 기반 탐지 엔진으로 스팸을 자동 분류한다.
사용자는 관리자 대시보드에서 결과를 검수하거나, 자동 숨김/삭제 정책을 설정할 수 있다.

## Architecture
- **Webhook Ingestion**: Instagram Graph API Webhook으로 실시간 댓글 수집
- **Normalization Pipeline**: 특수문자 제거, 초성 복원, 숫자→문자 치환, Unicode 정규화
- **Rule Engine**: 키워드 매칭, 패턴 점수화, 임계값 기반 스팸 판정
- **Async Moderation Queue**: Redis 기반 비동기 액션 큐, 구독 한도 초과 시 차단
- **Idempotent External Actions**: Instagram 숨김/삭제 API 호출은 idempotency key로 중복 방지
- **Audit Logging**: 모든 상태 변경 이벤트를 불변 로그로 기록
- **Token Encryption**: OAuth 토큰은 AES-256-GCM 암호화 후 저장

## Tech Stack
| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Security |
| Database | PostgreSQL 16, Flyway |
| Cache / Queue | Redis 7 |
| Build | Gradle, Testcontainers |
| Frontend | Next.js 14 (App Router), TypeScript, Tailwind CSS |
| Infra | Docker Compose, GitHub Actions, AWS (ECS / RDS / ElastiCache) |
| Observability | Spring Actuator, Micrometer, CloudWatch |

## Security
- OAuth 토큰 AES-256-GCM 암호화 저장
- 외부 응답에 내부 PK 미노출 (UUID 사용)
- 리소스 접근 시 소유권 검증
- Rate Limiting (Bucket4j)
- Audit Log (login, rule 변경, moderation 액션)
- Secret 마스킹 (로그에 토큰/헤더 미기록)
- OWASP ASVS L2 기준 체크리스트

## Demo Flow
1. 데모 댓글 임포트 (`eval/spam-samples.json`)
2. 스팸 탐지 실행 (정규화 → 룰 평가)
3. Moderation Queue에서 결과 검수
4. 숨김/삭제 액션 승인
5. Audit Log 확인

## API Docs
- 로컬: `http://localhost:8080/swagger-ui.html`
- 명세: `docs/api-contract.md`

## Running Locally
```bash
cp .env.example .env
docker compose up -d
# backend
cd backend && ./gradlew bootRun
# frontend
cd frontend && npm install && npm run dev
```

## Testing
```bash
cd backend && ./gradlew test          # 단위 + 통합 테스트 (Testcontainers)
cd backend && ./gradlew test --info   # 상세 출력
```
- 단위 테스트: 도메인 로직, 정규화 파이프라인, 룰 엔진
- 통합 테스트: Repository, 트랜잭션, Webhook 처리 (Testcontainers PostgreSQL)
- Mock 어댑터: Instagram API 실제 호출 없이 테스트

## Portfolio Highlights
| 항목 | 구현 내용 |
|---|---|
| 외부 API 신뢰성 | Circuit Breaker + Retry + Timeout (Resilience4j) |
| 멱등성 | Webhook 중복 수신, Instagram API 중복 액션 방지 |
| Rate Limit | 공개 API Bucket4j, Instagram API 한도 추적 |
| 토큰 보안 | AES-256-GCM 암호화, 복호화 범위 제한 |
| 감사 가능성 | 금융권 수준 불변 Audit Log |
| 구독 과금 제어 | 플랜별 월 처리 한도, 초과 시 액션 차단 |
| 테스트 전략 | 단위/통합 분리, Testcontainers, Mock 어댑터 |
