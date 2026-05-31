# workflow/traceability-matrix.md

## YSDA 추적성 매트릭스 (Traceability Matrix)

이 매트릭스는 MDRelay 핵심 요구사항/동작이 어떤 스펙, 아키텍처 결정(ADR), 품질 지표(Quality Bar) 및 검증 케이스와 정합성을 맺고 있는지 나타냅니다.

| 요구사항 (Feature / Requirement) | Spec 문서 | 아키텍처 결정 (ADR) | 품질 지표 (Quality Bar) | 테스트 케이스 (QA/TC / Unit) | 상태 (Status) |
|---|---|---|---|---|---|
| **Fast Launch & Smooth Viewing** | - | `ADR-000` §Baseline | `Simplicity & Maintainability` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) §1 | `Active` |
| **Intent VIEW (.md/.json/.txt)** | - | `ADR-000` §Intent | `Data Safety & Privacy` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) §2 | `Active` |
| **Intent SEND (Shared Text/File)** | - | `ADR-000` §Intent | `Data Safety & Privacy` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) §3 | `Active` |
| **FolderSync 연동** | - | `ADR-000` §FolderSync | `Data Safety` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) §4 | `Active` |
| **SAF Outbox (임시 저장/공유)** | - | `ADR-000` §SAF | `Data Safety & Privacy` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) §5 | `Active` |
| **Monospace Font for Code** | - | `ADR-000` §Font | `Simplicity` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) §6 | `Active` |
| **자동 빌드 정합 검사 및 보존** | - | `ADR-001` §CI | `Maintainability` | [android-ci.yml](file:///d:/Project/MDRelay/.github/workflows/android-ci.yml) | `Active` |
| **URL 감지 검증 (`detectSharedUrl`)** | - | `ADR-002` §Utility | `Testability & Observability` | [UtilityTest.kt](file:///d:/Project/MDRelay/app/src/test/java/com/simpsonys/mdrelay/UtilityTest.kt) §testDetectSharedUrl | `Active` |
| **파일명 정제 검증 (`sanitizeFilenameSegment`)** | - | `ADR-002` §Utility | `Data Safety & Testability` | [UtilityTest.kt](file:///d:/Project/MDRelay/app/src/test/java/com/simpsonys/mdrelay/UtilityTest.kt) §testSanitizeFilenameSegment | `Active` |
| **원격 Gist 딥링크 열기 (`mdrelay://web-open`)** | - | `ADR-003` §WebOpen | `Data Safety & Privacy` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) §2 | `Active` |
| **원격 파일명 파싱 검증 (`extractFilenameFromUrl`)** | - | `ADR-003` §WebOpen | `Testability & Data Safety` | [UtilityTest.kt](file:///d:/Project/MDRelay/app/src/test/java/com/simpsonys/mdrelay/UtilityTest.kt) §testExtractFilenameFromUrl | `Active` |
