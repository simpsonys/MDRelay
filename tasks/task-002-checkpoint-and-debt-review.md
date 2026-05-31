# tasks/task-002-checkpoint-and-debt-review.md

## 1. 목적 (Purpose)
MDRelay 프로젝트 도입기(Adoption) 체크포인트인 **자동화 CI 구성(ADR-CAND-001)** 및 **Unit Test 인프라 도입(DEBT-001)**을 마저 해결하여 프로젝트 개발 안정성과 검증 수준을 YSDA v2.8.5 규격에 도달시킵니다.

---

## 2. 연결 관계 (Traceability)
* **결정:** `arch/adr-001-automatic-ci-and-retention.md`, `arch/adr-002-unit-test-infrastructure.md`
* **품질 지표:** Data Safety, Privacy, Testability, Observability (`quality/quality-bar.md`)
* **테스트:** `app/src/test/java/com/simpsonys/mdrelay/UtilityTest.kt`

---

## 3. 작업 상세 내용 (DoD - Definition of DoD)
* [x] **CI 워크플로 구축:** `.github/workflows/android-ci.yml` 파일을 작성하여 push/pull_request 트리거에 연동. unsigned 디버그 APK를 빌드하고 아티팩트 보존 기한을 3일로 설정.
* [x] **JUnit 4 의존성 탑재:** `app/build.gradle`에 `testImplementation 'junit:junit:4.13.2'` 의존성을 추가해 단위 테스트 구동 체계 마련.
* [x] **MainActivity 헬퍼 가시성 조정:** `detectSharedUrl` 및 `sanitizeFilenameSegment`를 `internal`로 가시성 완화.
* [x] **단위 테스트 코드 수록:** `app/src/test/java/com/simpsonys/mdrelay/UtilityTest.kt` 파일을 작성하여 두 함수를 다각도로 검증.
* [x] **로컬 검증:** `.\ysdadev.cmd check` 명령어 구동 시 빌드와 유닛 테스트가 모두 통과됨을 입증.
* [x] **대시보드 업데이트:** `workflow/STATUS.md`, `workflow/artifact-registry.md`, `workflow/traceability-matrix.md` 갱신.
* [x] **로컬 커밋 완료:** `chore: resolve checkpoints and test debt` 메시지로 커밋.

---

## 4. 복구 시나리오 (Rollback Plan)
* `git reset --hard HEAD` 및 `git clean -fd`를 수행하여 Phase 2 변경 전 커밋 시점(`eb35f54`)으로 온전히 복구합니다.
