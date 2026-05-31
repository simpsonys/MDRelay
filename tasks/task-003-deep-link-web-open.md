# tasks/task-003-deep-link-web-open.md

## 1. 목적 (Purpose)
Obsidian 등 외부 생산성 도구에서 생성된 원격 마크다운/텍스트/JSON 문서를 딥링크 스키마(`mdrelay://web-open?url={encoded_url}`)를 통해 MDRelay로 유기적으로 보내, 백그라운드 다운로드를 수행하여 뷰어에 인메모리 프리뷰로 안전하게 적재하는 기능을 추가합니다.

---

## 2. 연결 관계 (Traceability)
* **결정:** `arch/adr-003-web-url-intake-scheme.md`
* **품질 지표:** Data Safety, Privacy, Testability, Observability (`quality/quality-bar.md`)
* **테스트:** `app/src/test/java/com/simpsonys/mdrelay/UtilityTest.kt`

---

## 3. 작업 상세 내용 (DoD - Definition of DoD)
* [x] **ADR 승인 전환:** `arch/adr-003-web-url-intake-scheme.md` 상태를 Proposed ➔ Accepted로 갱신.
* [x] **Manifest 수정:** `AndroidManifest.xml`에 인터넷 권한(`android.permission.INTERNET`)과 `mdrelay://web-open` 스키마 수신용 `intent-filter` 선언 추가.
* [x] **MainActivity 딥링크 분석 및 다운로드 구현:** `MainActivity.kt` 내에 `mdrelay://web-open` 인입 시 URL 파싱, URL 디코딩, 백그라운드 코루틴 `java.net.URL.openStream()` 파일 획득 논리 추가.
* [x] **다운로드 예외 처리 및 UI 알림:** 타임아웃, 404/500 에러 발생 시 UI 상에 Toast로 안전하게 전파하여 에러 은폐 방지.
* [x] **단위 테스트 수록:** `UtilityTest.kt`에 URL 디코딩 및 파일명 파싱 유틸리티 테스트 케이스 수록 및 정합 검증.
* [x] **로컬 검증:** `.\ysdadev.cmd check` 실행으로 단위 테스트가 모두 패스하는지 확인.
* [x] **대시보드 업데이트:** `workflow/STATUS.md`, `workflow/artifact-registry.md`, `workflow/traceability-matrix.md` 갱신.
* [x] **로컬 커밋 완료:** `feat: support mdrelay://web-open deep link scheme` 커밋 메시지로 형상 관리 완료.

---

## 4. 복구 시나리오 (Rollback Plan)
* `git reset --hard HEAD` 및 `git clean -fd`를 통해 Phase 3 시작 전 커밋 시점(`863417d` 혹은 태스크 갱신 후 커밋)으로 완벽 복구합니다.
