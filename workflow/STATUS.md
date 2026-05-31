# workflow/STATUS.md

## 마일스톤: YSDA Harness v2.8.5 Adoption & Deep Link Web Open Completed

- **상태:** `[x] Completed`
- **목표:** YSDA Harness 도입과 체크포인트 해결 완료 및 `mdrelay://web-open?url=...` 딥링크를 통한 원격 웹 문서 안전 인입/다운로드 구현 완료.
- **완료 일자:** 2026-05-31

---

## 1. 활성 작업 상태 (Active Tasks)

*(현재 활성화된 대기 상태의 태스크가 없으며, 마일스톤의 모든 DoD가 정상 달성되었습니다)*

| Task ID | 작업 명칭 | 상태 | 담당 역할 | 비고 |
|---|---|---|---|---|
| `TASK-001` | YSDA Harness 2.8.5 Adoption | `[x] Completed` | 에이전트 | 구조/도구 세트 구축 완료 |
| `TASK-002` | Checkpoint & Debt Resolution | `[x] Completed` | 에이전트 | CI/CD 및 JUnit 유닛 테스트 완료 |
| `TASK-003` | Deep Link Web Open Scheme | `[x] Completed` | 에이전트 | 원격 딥링크 스키마 인입 완료 |

---

## 2. 체크포인트 (나중에 검토)

*(Adoption 단계의 하네스 체크포인트 및 기술 부채 전원 해결 완료)*

- [x] `ADR-CAND-001` (Medium): **CI/CD 및 Rolling Release 자동화** ➔ `ADR-001` 및 `.github/workflows/android-ci.yml`을 통해 해결 완료.
- [x] `DEBT-001` (Low): **Android Unit Test 폴더 부재** ➔ `ADR-002` 및 `app/src/test` 밑 `UtilityTest.kt` 탑재로 해결 완료.

---

## 3. 마일스톤 연혁 (History)

* **2026-05-31:**
  * YSDA Harness v2.8.5 및 2개 체크포인트(CI/CD, Unit Test) 도입 및 형상 커밋 완료 (`TASK-001`, `TASK-002`).
  * **원격 딥링크 기능 추가 (`TASK-003`)**: Obsidian과 같은 외부 앱으로부터 `mdrelay://web-open?url={encoded_url}` 형태의 딥링크 스키마를 인입받아 원격 문서를 무의존성 백그라운드 코루틴으로 다운로드/뷰잉하는 요구사항 반영 완료.
  * 매니페스트 인터넷 권한 추가, 딥링크용 Intent Filter 추가, 코루틴 비동기 `HttpURLConnection` 다운로드 및 인메모리 프리뷰 적재 로직 완료.
  * URL 디코딩 및 확장자 필터링 정합 검증 유틸리티 단위 테스트 3개 케이스 추가 수록 및 로컬 검증(`ysdadev check` 총 14개 케이스) 통과 완료.
