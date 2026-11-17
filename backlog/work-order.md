<!-- work-order-hash: 86bcc2c181602fd00a0d7396187d04c055c4f099983f4dde62b5d86fbea514d6 -->
# 작업 순서

`work_order.py`가 backlog/tasks의 의존(`dependencies`)으로 만든 파일이다 — 직접 고치지 말고 다시 생성한다. 같은 단계의 태스크는 병렬로, 단계는 위에서 아래로 순차로 진행한다. Done 태스크와 없는 태스크에 대한 의존은 충족된 것으로 본다.

| 단계 | ID | 제목 |
|---|---|---|
| 1 | AO-6 | 태스크 브랜치의 git-logbook 트레일러를 읽는 CommitLedger를 만든다 |
| 1 | AO-20 | RunCommand 테스트를 먼저 쓴다 |
| 1 | AO-31 | 최상위 TASK-21 전파: design/ 주의점을 AO-30 이후 코드와 최상위 BL-002 · ORC-021에 맞춘다 |
| 2 | AO-7 | run 흐름을 배선하고 실제 토큰을 카드와 runs.jsonl에 기록한다 |
| 2 | AO-21 | E2E 스모크 확인 항목 골격을 먼저 쓴다 |
| 3 | AO-8 | E2E 픽스처로 예상 토큰부터 실제치 병기까지 실제 claude로 검증한다 |
