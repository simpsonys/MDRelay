# workflow/STATUS.md

## 마일스톤: YSDA Harness v2.8.5 Adoption (Standard Mode)

- **상태:** `[/] In-Progress`
- **목표:** 기존 Android MDRelay 프로젝트에 YSDA Harness 규격 적용 및 `ysdadev` façade 준비, 구조/품질 바 완성.
- **기간:** 2026-05-31 ~ 2026-06-01

---

## 1. 활성 작업 상태 (Active Tasks)

| Task ID | 작업 명칭 | 상태 | 담당 역할 | 비고 |
|---|---|---|---|---|
| `TASK-001` | YSDA Harness 2.8.5 Adoption | `In-Progress` | 에이전트 (Implementer) | 스냅샷 준비 완료, 대시보드 작성 중 |

---

## 2. 체크포인트 (나중에 검토)

- [ ] `ADR-CAND-001` (Medium): **CI/CD 및 Rolling Release 자동화**
  * **내용:** GitHub Actions 워크플로 및 unsigned debug/release 빌드 아티팩트 보존(retention-days: 3일) 도입.
  * **트리거:** 다음 마일스톤 시작 시 혹은 Owner가 자동 빌드 아티팩트 공유를 원할 때 검토.
- [ ] `DEBT-001` (Low): **Android Unit Test 폴더 부재**
  * **내용:** 현존 테스트 로직이 전무하므로 수동 검증 가이드라인(`qa/manual-test-checklist.md`) 위주로 검증.
  * **트리거:** 핵심 파싱 유틸리티 함수나 상태 기계 로직이 분리 설계되는 시점에 `test unit`에 실질 테스트 추가 검토.

---

## 3. 마일스톤 연혁 (History)

* **2026-05-31:** 기존 MDRelay 분석 완료. YSDA Harness v2.8.5 Standard Mode 적용 계획 승인 완료. `.ysda-harness/` 스냅샷 구성 및 `AGENTS.md` 갱신.
