---
id: doc-1
title: 팩토리 문서 검증 로그
type: specification
created_date: '2026-10-03 03:15'
---

# 팩토리 문서 검증 로그

coralstay-software-factory(`~/githubs/coralstay-software-factory`) 문서의 가설·불일치를 이 저장소 구현으로 검증하고,
그 결과를 팩토리 문서에 반영한 상태를 기록한다. **검증 상태는 이 표에서만 관리한다**(팩토리 README 규칙 4와 같은 원칙).

상태: ⏳ 미착수 · 🔍 실측 중 · ✅ 반영 완료(팩토리 커밋 해시 기재) · ❌ 가설 기각

| #  | 팩토리 위치 | 문제 | 확인·반영할 것 | 담당 태스크 | 상태 |
|----|-------------|------|----------------|-------------|------|
| D1 | 아키텍처.md 3절, README "한눈에 보는 실행 기반" 그림 | "Java 22+" ↔ RFC 06절 "Java 25" | Java 25로 통일 (decision-1) | Gradle 골격 + CLI 진입 | ✅ 8931057 |
| D2 | 아키텍처.md 1·4절 계층 1 | `WatchService` 감시를 확정처럼 기술 ↔ RFC 09절 "CLI 폴링 vs WatchService 미정" | 미정으로 정정, 스켈레톤은 단발 CLI (decision-2) | run 배선 + 기록 | ⏳ |
| D3 | 아키텍처.md 6절, README 6절 디렉토리 구조 | `allocator/interceptor/parser/web` ↔ RFC 06절 `Watcher/Queue/Policy/Spawner/Registry` | 실제 패키지 구조로 갱신 | worktree 관리 | ✅ 6a20bc0 |
| D4 | RFC 04절 "회수 시 컨텍스트 폐기", fig.5(`diagrams/07-task-sequence.mmd`), README 로드맵 1 | "`/clear` 후 회수" — `claude -p` 헤드리스는 프로세스 종료가 곧 컨텍스트 폐기라 `/clear` 단계가 성립하지 않을 수 있음 | 회수 = 프로세스(자손 포함) 종료 + Registry 제거로 실측 후 문구 정정 | 에이전트 스폰, 세션 레지스트리 | ✅ 46d3e10 + d71e2b7 |
| D5 | RFC 00·02·06·07·12절, README, 아키텍처.md, `diagrams/07-task-sequence.mmd`, `diagrams/02-bootstrap.dot` | 도구 명칭이 git-format으로 남아 있음. 토큰 측정 주체·형식 서술 필요 | 명칭을 git-trail로, "외부 도구가 커밋 트레일러(`Tokens-Used: in=N out=M`)로 남기고 오케스트레이터는 읽기만"으로 정정. 헤드리스 실측은 2026-10-03 스파이크로 확인(`in=59858 out=465`, 트랜스크립트 합산과 일치) | 커밋 원장 판독(AO-6) | 🔍 헤드리스 실측 확인 · 문서 반영 대기 |
| D6 | 아키텍처.md 4절 4단계 | "Dev 모드가 스스로 done 전환" ↔ RFC 04·07절은 Verifier 판정 후 Done | 스켈레톤은 Implementer 단독 + claude-rails 게이트임을 명시 | run 배선 + 기록 | ⏳ |
| D7 | README 로드맵 2 "실측 3종" 중 backlog.md 필드 | 의존성/마일스톤/상태 필드 실제 지원 범위 미확인 | backlog 1.51.0 `task view --plain` 출력의 deps·status 파싱 가능 여부 기록 | backlog 클라이언트 | ✅ aaa0b0a |
| D8 | README 상단 경고문 | "구현 코드는 한 줄도 없다" | agent-orchestartor 링크 + 스켈레톤 범위로 갱신 | run 배선 + 기록 | ⏳ |
| D9 | README 로드맵 2 "실측 3종" 중 peak RSS | `claude` 프로세스 1개 peak RSS 미측정 (Semaphore 상한 근거) | 2026-10-03 스파이크(사소한 태스크 1개): 240~290MB, 17~19초, $0.12~0.13. E2E에서 실제 태스크 규모로 재측정 후 팩토리 기록 | E2E 픽스처(AO-8) | 🔍 1차 실측 |
| D10 | RFC 04·08절, 아키텍처.md 4절 | 카드에 예상 토큰 필드가 없음 | 카드 Description의 `### Token Budget` 소절(estimated / attempt n / actual) 스키마와 수명 주기 서술 신설 (decision-6) | BacklogCardSource | ⏳ |
| D11 | RFC 06절 오케스트레이터, fig.7(`09-orchestrator.dot`) | 예상 토큰 → 모델 배정, claim, 실제치 기록 흐름이 없음 | 실행 순서(카드 검증 → 판정 → 정책 → claim → 워크트리 → 실행 → 실제치 → 브랜치 상태 판정) 서술·다이어그램 반영 (decision-9·10) | run 배선(AO-7) | ⏳ |
| D12 | RFC 06절 "역할 하나당 다른 프로바이더로 교체 가능", README 왜·어떻게 표 | 다중 프로바이더를 기본 방향처럼 서술 | 범위는 Claude Code 전용, 실행부만 AgentRunner 포트로 분리한다고 정정 (decision-7) | AgentRunner | ⏳ |
| D13 | RFC 06절 토큰 측정 | 커밋 트레일러 합계를 태스크 총량처럼 서술 | 트레일러는 마지막 커밋 뒤 턴을 포함하지 않음(스파이크: 입력 약 26% 누락) → 커밋 귀속분과 세션 총량 병기로 정정 (decision-8) | run 배선(AO-7) | ⏳ |

## 기록 규칙

- 행을 추가할 때는 팩토리 파일·절을 정확히 적는다.
- ✅로 바꿀 때 팩토리 저장소 `docs/orchestrator-skeleton` 브랜치의 커밋 해시를 상태 칸에 함께 적는다.
- 실측값은 이 표가 아니라 팩토리 README "출처와 검증 상태" 표와 RFC 본문에 둔다 — 여기는 상태만.
