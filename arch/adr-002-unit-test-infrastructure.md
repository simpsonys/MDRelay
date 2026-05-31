# arch/adr-002-unit-test-infrastructure.md

- **Status:** `Accepted`
- **Driver:** YSDA Common Core §C13 & §C16 & §C25
- **날짜:** 2026-05-31

---

## 1. Context & Decision Brief (맥락 및 의사결정 브리프)

MDRelay 프로젝트는 그동안 `app/src/test` 디렉터리가 부재하여 로컬 유닛 테스트 자동 검증이 불가능한 기술 부채를 안고 있었습니다. 이로 인해 `MainActivity.kt` 등 핵심 파이프라인에서 파일 인코딩 처리나 Intent 파싱 오류, 확장자 정제 오류가 생겼을 때 디바이스 수동 실행에만 의존해야 했습니다.

이 기술 부채(`DEBT-001`)를 해결하고, 향후 기능 개발에 안전 장치를 달아주기 위해 **Kotlin + JUnit 4 단위 테스트 인프라**를 공식 **Accepted**로 명문화하여 도입합니다.

---

## 2. Decision Drivers (결정 제약)

* **제품 안전성(Product Safety):** 소스 코드의 컴파일 흐름과 런타임 크기(APK size)에 절대 유해한 영향을 미쳐선 안 됩니다. (테스트에만 결합)
* **테스트 용이성(Testability):** `MainActivity.kt` 내부에 존재하는 정적/동적 순수 유틸리티 헬퍼 함수들이 안드로이드 컨텍스트 모킹(Mocking) 없이 신속히 독립 검증될 수 있어야 합니다.

---

## 3. 대안 비교 (Alternatives)

| 대안 | 제품 안전성 | 독립 테스트 용이성 | 복잡도 | 비고 |
|---|---|---|---|---|
| **안 1. MainActivity 내에 테스트용 뷰 버튼 추가** | `Low` (출시본에 불필요한 테스트 UI와 로직 유입) | `Low` (수동 클릭 필요) | `Low` | 비권장 (보안/청결 위배) |
| **안 2. JUnit 4 의존성 탑재 및 헬퍼 함수 가시성 완화** | `High` (testImplementation 지정으로 APK 완전 배제) | `High` (Android Context 없이 0.1초 만에 로컬 검증) | `Low` | **채택** (`private` ➔ `internal`) |
| **안 3. Robolectric 모킹 프레임워크 전면 도입** | `High` | `Medium` (테스트 구동 속도가 비교적 느림) | `High` | 오버스펙 (후순위 검토 가능) |

---

## 4. 세부 적용 사항 (Implications)

1. **테스트 가시성 조정:** `MainActivity.kt` 최하단에 있는 `detectSharedUrl` 및 `sanitizeFilenameSegment` 함수는 안드로이드 SDK 의존성이 없는 순수 코틀린 데이터 연산이므로, 가시성을 `private`에서 `internal`로 전환하여 동일 패키지 테스트 스위트가 접근할 수 있게 합니다.
2. **테스트 패키지 위치:** `app/src/test/java/com/simpsonys/mdrelay/` 아래에 `UtilityTest.kt`를 생성합니다.
3. **Façade 연동:** `.\ysdadev.cmd test unit` 실행 시 `gradlew.bat testDebugUnitTest` 명령어가 자동 호출되어 테스트가 정상 검증됩니다.
