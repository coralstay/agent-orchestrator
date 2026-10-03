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
| D4 | RFC 04절 "회수 시 컨텍스트 폐기", fig.5(`diagrams/07-task-sequence.mmd`), README 로드맵 1 | "`/clear` 후 회수" — `claude -p` 헤드리스는 프로세스 종료가 곧 컨텍스트 폐기라 `/clear` 단계가 성립하지 않을 수 있음 | 회수 = 프로세스(자손 포함) 종료 + Registry 제거로 실측 후 문구 정정 | 에이전트 스폰, 세션 레지스트리 | 🔍 스폰 측 반영 46d3e10 · 회수 측 AO-5 |
| D5 | RFC 00·02·06·07·12절, README, `diagrams/07-task-sequence.mmd` | 도구명 "git-format" → git-logbook(git-format decision-25). 토큰 측정 주체는 git-logbook `post-commit` 트레일러 | 명칭 정정 + `claude -p` 헤드리스 세션에서 `Tokens-Used`가 실측값인지/어떤 `unavailable` 사유인지 실측 (decision-5) | 커밋 원장 판독, E2E 픽스처 | ⏳ |
| D6 | 아키텍처.md 4절 4단계 | "Dev 모드가 스스로 done 전환" ↔ RFC 04·07절은 Verifier 판정 후 Done | 스켈레톤은 Implementer 단독 + claude-rails 게이트임을 명시 | run 배선 + 기록 | ⏳ |
| D7 | README 로드맵 2 "실측 3종" 중 backlog.md 필드 | 의존성/마일스톤/상태 필드 실제 지원 범위 미확인 | backlog 1.51.0 `task view --plain` 출력의 deps·status 파싱 가능 여부 기록 | backlog 클라이언트 | ✅ aaa0b0a |
| D8 | README 상단 경고문 | "구현 코드는 한 줄도 없다" | agent-orchestartor 링크 + 스켈레톤 범위로 갱신 | run 배선 + 기록 | ⏳ |
| D9 | README 로드맵 2 "실측 3종" 중 peak RSS | `claude` 프로세스 1개 peak RSS 미측정 (Semaphore 상한 근거) | `/usr/bin/time -l`로 E2E 1회 측정해 기록 | E2E 픽스처 | ⏳ |

## 기록 규칙

- 행을 추가할 때는 팩토리 파일·절을 정확히 적는다.
- ✅로 바꿀 때 팩토리 저장소 `docs/orchestrator-skeleton` 브랜치의 커밋 해시를 상태 칸에 함께 적는다.
- 실측값은 이 표가 아니라 팩토리 README "출처와 검증 상태" 표와 RFC 본문에 둔다 — 여기는 상태만.
