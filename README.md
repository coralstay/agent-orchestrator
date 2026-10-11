# agent-orchestartor

[coralstay-software-factory](https://github.com/coralstay/coralstay-software-factory) 설계 중
오케스트레이터만 구현한 JVM 오케스트레이터 워킹스켈레톤이다. CLI `orchestrator run <taskId>`로
태스크 하나를 실행한다. 빌드·테스트는 Java 25에서 `./gradlew test`.
아직 구현이 없는 `@Tag("pending")` 테스트는 `test`에서 빠지며 `./gradlew pendingTest`로 따로 돌린다.

## 라이선스

[GNU AGPL-3.0](./LICENSE) — 저장소 전체에 적용된다(전문: [`LICENSE`](./LICENSE), [GNU AGPL-3.0 원문](https://www.gnu.org/licenses/agpl-3.0.html)). 네트워크 서비스 제공이나 상용 사용 시에도
AGPL 조건에 따라 수정 소스를 공개해야 한다.

- **상용 라이선스 —** 저작권자가 단독이므로 듀얼 라이선스가 가능하다. AGPL 조건(네트워크
  서비스 제공 시 수정 소스 공개)이 맞지 않는 상용 사용은 별도 협의한다.
  문의: <coralstay3595@gmail.com> (또는 이 저장소의 GitHub Issue).
- **기여 —** 듀얼 라이선스 유지를 위해, 기여자는 자신의 기여를 AGPL-3.0과 상용 라이선스 양쪽으로
  배포할 권리를 저작권자에게 허여하는 데 동의한 것으로 본다.
