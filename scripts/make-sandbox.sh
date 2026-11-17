#!/bin/sh
# ---
# title: make-sandbox
# description: agent-orchestrator run을 실제로 돌려 볼 일회용 샌드박스 저장소(카드 SB-1 하나)를 만드는 스크립트
# authors: [coralstay]
# license: AGPL-3.0-only
# status: draft
# created_date: '2026-11-17'
# updated_date: '2026-11-17'
# task: AO-33
# ---
# AO-33 AC #6: 원격 없는 throwaway 저장소를 만들고 backlog를 초기화한 뒤, 헤드리스 에이전트가 처리할 To Do 카드
# 'hello.txt 를 만든다'(AC 1개 · DoD 1개 · SOAP 계획)를 드래프트 → 승격으로 만들어 main에 커밋한다.
# 사용법: sh scripts/make-sandbox.sh <경로>      (예: ~/githubs-work/ao-sandbox)
# 커밋은 사용자 git 설정 그대로 한다. 계획 승인 서명(Plan-Approved)은 사람이 직접 한다 — 마지막에 명령을 안내한다.
set -eu

if [ $# -ne 1 ] || [ -z "$1" ]; then
    echo "usage: sh scripts/make-sandbox.sh <경로>" >&2
    exit 2
fi
DIR=$1
if [ -e "$DIR" ]; then  # ---- 1
    echo "이미 있습니다(지우지 않습니다): $DIR" >&2
    exit 1
fi

mkdir -p "$DIR"
cd "$DIR"
git init -q -b main  # ---- 2
# init은 원격이 없다는 경고를 stderr에 찍는다 — 바로 아래에서 원격 조회를 끄므로 출력은 버리고 실패만 알린다
backlog init ao-sandbox --defaults --integration-mode none --task-prefix SB >/dev/null 2>&1 \
    || { echo "backlog init 실패: $DIR" >&2; exit 1; }
backlog config set remoteOperations false >/dev/null
backlog config set autoOpenBrowser false >/dev/null

PLAN='### Subjective
agent-orchestrator 최소 run(AO-33)을 실제로 한 번 돌려 보기 위한 샌드박스 카드다.

### Objective
- 저장소에는 backlog 설정과 이 카드만 있다.
- hello.txt 는 아직 없다.

### Assessment
파일 하나를 만드는 가장 작은 작업으로 착수 · AC 체크 · Done · 커밋 흐름만 확인한다.

### Plan
1. 저장소 루트에 hello.txt 를 만들고 내용을 hello 한 줄로 쓴다.
2. 커밋한다.'

CREATED=$(backlog draft create "hello.txt 를 만든다" \
    -d "agent-orchestrator 최소 run(AO-33) 실제 실행용 샌드박스 카드.")  # ---- 3
DRAFT=$(printf '%s\n' "$CREATED" | sed -n 's/^Created draft \([^ ]*\).*$/\1/p')
if [ -z "$DRAFT" ]; then
    echo "드래프트 ID를 읽지 못했습니다: $CREATED" >&2
    exit 1
fi
backlog draft edit "$DRAFT" \
    --ac "저장소 루트에 hello.txt 가 있고 내용이 'hello' 한 줄이다" \
    --dod "hello.txt 를 만든 커밋이 태스크 브랜치에 있다" \
    --plan "$PLAN" >/dev/null
backlog draft promote "$DRAFT" >/dev/null
ID=$(sed -n 's/^id: *//p' backlog/tasks/*.md | head -n 1 | tr -d "'\"")  # ---- 4

git add -A
git commit -q -m "chore: ao-sandbox 초기화 ($ID 'hello.txt 를 만든다')"

echo "샌드박스: $(pwd)"
echo "태스크: $ID (To Do)"
echo "다음 단계 — 사람이 자기 키로 계획 승인 서명을 남긴다(헤드리스 에이전트는 서명할 수 없다):"  # ---- 5
echo "  cd $(pwd) && git commit --allow-empty -S -m \"Plan-Approved: $ID\""
echo "그 뒤 같은 디렉토리에서: agent-orchestrator run $ID"

# ======== 노트 ========
# 1 이미 있으면 손대지 않는다
#   설명: 경로가 있으면(빈 디렉토리여도) 아무것도 만들거나 지우지 않고 실패한다. 다시 만들려면 사람이 직접 지운다.
# 2 원격 없는 main 저장소
#   설명: git init -b main으로 시작한다. 원격이 없어 에이전트가 push를 시도해도 갈 곳이 없다. backlog의 원격 조회와 브라우저 자동 열기는 끈다.
#   관련: AO-33
# 3 드래프트 → 승격
#   설명: backlog task create로 바로 만들지 않고 draft create → draft edit(AC · DoD · SOAP 계획) → draft promote 순서를 따른다. 에이전트 세션의 In Progress 전환은 계획 · AC · DoD가 있어야 훅을 통과한다.
#   관련: require_draft_first, require_implementation_plan
# 4 태스크 ID는 카드 파일에서 읽는다
#   설명: promote 출력 형식에 기대지 않고 backlog/tasks의 카드 frontmatter id를 읽는다. 접두사 SB라 SB-1이 된다.
# 5 서명은 사람이 한다
#   설명: 착수 검사는 HEAD에서 닿는 커밋 중 사람 키로 서명된 'Plan-Approved: <ID>'를 요구한다. 스크립트는 서명하지 않고 명령만 안내한다. 워크트리는 main HEAD에서 갈라지므로 서명은 run 전에 main에 있어야 한다.
#   관련: decision-14
