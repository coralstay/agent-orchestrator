---
id: DRAFT-9
title: '최소 run: 에이전트 1개를 실제로 띄워 카드 1장을 처리한다'
status: Draft
assignee: []
created_date: '2026-11-17T13:58:00Z'
labels:
  - run
  - e2e
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## 배경
유저(2026-11-17): 오케스트레이터 run 을 작게, 에이전트 하나만 띄워 실제로 실행해 보고 계속 발전시킨다. AO-7(전체 run 10단계)의 얇은 한 조각으로, 이미 구현된 부품(BacklogClient·WorktreeManager·AgentSpawner·SessionRegistry)만 잇는다.
계획: ~/.claude/plans/agent-orchestrator-min-run.md (승인됨)

## 흐름 (decision-2·3·7·14)
1. 대상 저장소 = 현재 디렉터리, HEAD 가 main 아니면 exit 3
2. BacklogClient.view + isRunnable(To Do·의존 Done·미병합 task/<id> 없음) 아니면 사유와 exit 3, 스폰 없음
3. WorktreeManager.create(id) → <repo>-wt/<id>, task/<id>
4. SessionRegistry.admit → AgentSpawner.start(워크트리, 고정 프롬프트, timeout) → await, retire 는 finally
5. 판정: 워크트리 카드 Done · AC 전부 체크 · main..task/<id> 커밋 수 ≥ AC 수 → 성공, To Do 그대로면 '착수 안 됨'
6. 성공이면 워크트리 제거(브랜치 유지), 실패면 보존
7. 출력: 판정·종료코드·timedOut·소요·커밋 수 대 AC 수·워크트리. exit 0 성공 / 1 실패 / 2 usage / 3 실행 불가
- AgentSpawner.command 보강: --disallowedTools Bash(git push:*), --max-budget-usd(기본 1.00, ORCHESTRATOR_MAX_BUDGET_USD), --output-format json. --model 은 넣지 않음

## 비즈니스 규칙
- B1 (AC #1) 실행 불가면 워크트리도 프로세스도 만들지 않는다
- B2 (AC #2) 예외·타임아웃이 나도 retire 는 반드시 실행된다
- B3 (AC #3) 종료 코드 0만으로 성공 처리하지 않는다
- B4 (AC #4) 헤드리스 argv 에 push 금지와 비용 상한이 항상 들어간다
- B5 (AC #3) main 의 카드는 오케스트레이터가 고치지 않는다(decision-14)

## 샌드박스(AC #6)
scripts/make-sandbox.sh 로 ~/githubs-work/ao-sandbox(git·backlog init, 카드 SB-1 'hello.txt 를 만든다' AC 1개) 생성. 헤드리스 에이전트는 사람 서명을 못 하므로 유저가 샌드박스에서 Plan-Approved: SB-1 을 서명해 둔다. 실행 전 비용 상한을 유저에게 알리고 확인.

## 하지 않는 것
Token Budget 검증·기록, ModelPolicy, runs.jsonl, CommitLedger, ClaudeCodeRunner 포트 전환, 헤드리스 JSON 파싱(AO-7·AO-8·DRAFT-1~4)
<!-- SECTION:DESCRIPTION:END -->
