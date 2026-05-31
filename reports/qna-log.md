# reports/qna-log.md

## YSDA Session Q&A Log (MDRelay)

---

### Q1. MDRelay에 유닛 테스트 폴더(`app/src/test`)가 아예 부재한 상태입니다. `ysdadev.cmd test unit` 명령은 어떻게 대응해야 합니까?
* **A·결론:** Gradle Wrapper(`gradlew testDebugUnitTest`) 명령어로 매핑하되, 테스트가 없는 구조에서도 Gradle 빌드가 성공(Build Successful)으로 동작하므로 무난히 정상 매핑 처리합니다. 또한, 실기기 및 에뮬레이터 환경을 커버할 수 있는 철저한 `qa/manual-test-checklist.md` 가이드라인을 제공해 수동 검증 품질을 대체 보완합니다.
* **연결:** [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md), `scripts/ysdadev.py`

---

### Q2. 기존에 `DevToolMDRelay.ps1` 스크립트가 잘 작성되어 작동 중입니다. ysdadev façade를 완전히 새로 개발해야 합니까?
* **A·결론:** YSDA Adoption §E11 규정에 따라, 기존에 작동 중인 스크립트는 폐기하거나 지우지 않습니다. 다만 ysdadev 표준 명사 어휘(`list`, `doctor`, `build`, `test unit`, `install`, `run`, `check`)를 파이썬 Façade(`scripts/ysdadev.py`)가 수신한 뒤, 이에 맞추어 내부적으로 Gradle Wrapper 또는 adb를 정밀 호출하도록 매핑하여 개발 가독성과 범용성을 증대시킵니다.
* **연결:** `.ysda-harness/devtool.json`, `scripts/ysdadev.py`
