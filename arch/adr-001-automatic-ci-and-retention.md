# arch/adr-001-automatic-ci-and-retention.md

- **Status:** `Accepted`
- **Driver:** YSDA Common Core §C29 & Adoption §E14
- **날짜:** 2026-05-31

---

## 1. Context & Decision Brief (맥락 및 의사결정 브리프)

MDRelay 프로젝트는 그동안 tag push(`v*`) 또는 수동 실행 시에만 Keystore 서명을 포함한 정식 릴리즈 빌드를 배포해왔습니다. 그러나 개발 과정에서 `main` 브랜치에 코드가 푸시되었을 때 코드가 깨지지 않았는지(Build broken) 즉각 검증해주는 자동화 피드백 루프가 부재했습니다.

이에 따라, 빌드 무결성을 상시 확인하고 최신 테스트 실행 환경을 구축하기 위해 GitHub Actions 기반의 자동 검증 CI 파이프라인을 Proposed에서 **Accepted**로 선언하여 도입합니다.

---

## 2. Decision Drivers (결정 제약)

* **보안성(Keystore Isolation):** CI 수행 과정에서 절대 Keystore 서명 비밀번호나 서명 키 파일이 노출되거나 소스 트리에 올라가서는 안 됩니다.
* **리소스 효율성(Artifact Retention):** Android APK 아티팩트는 용량이 크므로(수십 MB) 매 빌드마다 장기 보존(기본 90일)하면 GitHub 스토리지 쿼터 압박을 줍니다. 이에 임시 디버그 아티팩트는 **3일 보존**으로 강력 제한합니다.

---

## 3. 대안 비교 (Alternatives)

| 대안 | 보안성 | 리소스 효율 | 복잡도 | 리스크 | 비고 |
|---|---|---|---|---|---|
| **안 1. 전역 push 시 signed release 빌드 수행** | `Low` (매번 secrets 로드 및 서명 유출 위협) | `Low` (용량 누적) | `High` | `High` | 키 유출 위협 상재 |
| **안 2. (선택) main push 시 unsigned debug 빌드 검증 및 3일 보존** | `High` (서명 키 아예 미사용) | `High` (3일 후 자동 증발) | `Low` (gradlew check 수행) | `Low` | **채택** (안전 및 속도 최적화) |
| **안 3. 외부 3rd-party CI 툴(Bitrise 등) 연동** | `Medium` | `Medium` | `High` (외부 연동 리스크) | `Medium` | 절차 복잡 |

---

## 4. 세부 적용 사항 (Implications)

1. **자동 트리거:** `main` 브랜치에 push 또는 pull_request가 발생하면 `android-ci.yml`이 즉각 런칭됩니다.
2. **unsigned debug build:** `./gradlew compileDebugKotlin assembleDebug testDebugUnitTest` 연동 검증.
3. **아티팩트 업로드:** `actions/upload-artifact@v4`를 이용하여 `app-debug.apk`를 업로드하되 `retention-days: 3`을 명시해 3일 후 자동 정제되도록 합니다.
