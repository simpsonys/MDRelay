# workflow/artifact-registry.md

## YSDA Artifact Registry (문서 관리 대장)

이 프로젝트에 존재하는 YSDA Harness 산출물의 고유 번호와 이력 상태를 추적 관리하는 마스터 레지스트리입니다.

---

## 1. 아키텍처 의사결정 문서 (ADR)

| ID | 파일 경로 | 상태 | 제목 | 승인일 | 비고 |
|---|---|---|---|---|---|
| `ADR-000` | [adr-000-existing-architecture-baseline.md](file:///d:/Project/MDRelay/arch/adr-000-existing-architecture-baseline.md) | `Accepted` | Existing Architecture Baseline | 2026-05-31 | 현존 MDRelay 핵심 아키텍처 및 제약 명시 |
| `ADR-001` | [adr-001-automatic-ci-and-retention.md](file:///d:/Project/MDRelay/arch/adr-001-automatic-ci-and-retention.md) | `Accepted` | Automatic CI and Artifact Retention | 2026-05-31 | 무서명 디버그 빌드 검증 및 3일 보존 자동화 |
| `ADR-002` | [adr-002-unit-test-infrastructure.md](file:///d:/Project/MDRelay/arch/adr-002-unit-test-infrastructure.md) | `Accepted` | Unit Test Infrastructure | 2026-05-31 | JUnit 4 테스트 환경 마련 및 가시성 완화 |
| `ADR-003` | [adr-003-web-url-intake-scheme.md](file:///d:/Project/MDRelay/arch/adr-003-web-url-intake-scheme.md) | `Accepted` | Deep Link Web Open Scheme | 2026-05-31 | 원격 딥링크 스키마 인입 및 다운로드 설계안 |

---

## 2. 세부 스펙 문서 (Spec)

*(현재 승인된 신규 스펙 사양은 존재하지 않습니다. 기능 추가 시 작성 예정)*

---

## 3. 품질 및 검증 문서 (QA / Quality)

| ID | 파일 경로 | 상태 | 제목 | 비고 |
|---|---|---|---|---|
| `QB-001` | [quality-bar.md](file:///d:/Project/MDRelay/quality/quality-bar.md) | `Active` | MDRelay Quality Bar | Intent 및 SAF, FolderSync 대응 수록 |
| `TC-001` | [manual-test-checklist.md](file:///d:/Project/MDRelay/qa/manual-test-checklist.md) | `Active` | MDRelay Manual Test Checklist | CLI/adb 및 sample 파일 기반 검증 목록 |
| `UT-001` | [UtilityTest.kt](file:///d:/Project/MDRelay/app/src/test/java/com/simpsonys/mdrelay/UtilityTest.kt) | `Active` | MainActivity Utility Unit Test | 헬퍼 함수 2개에 대한 JUnit 테스트 케이스 |

---

## 4. 진척 및 Q&A 로그 (Reports)

| ID | 파일 경로 | 상태 | 제목 | 비고 |
|---|---|---|---|---|
| `RP-001` | [progress.md](file:///d:/Project/MDRelay/reports/progress.md) | `Active` | Harness Adoption Progress Report | 진척도 요약 |
| `QL-001` | [qna-log.md](file:///d:/Project/MDRelay/reports/qna-log.md) | `Active` | Session Q&A Log | 주요 질의 및 의사결정 추적 |
