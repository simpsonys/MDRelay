# .ysda-harness/devtool.md

## MDRelay Local DevTool Façade Guide (`ysdadev`)

이 문서는 MDRelay 프로젝트에 로컬로 정의된 `ysdadev` façade의 명령어 활용 방법과 라이프사이클 작동 방식을 설명합니다. 

이 도구는 전역 도구에 의존하지 않으며, 오직 **프로젝트 루트에 직접 선언된 명령어**로만 명시적 구동을 전제합니다.

---

## 1. 공식 실행 구문 (Official Invocation)

운영체제 및 셸 환경에 따라 다음과 같이 절대적/상대적인 명시 경로로 호출합니다:

* **Windows PowerShell / Cmd:**
  ```powershell
  .\ysdadev.cmd <command>
  ```
* **macOS / Linux / WSL Bash:**
  ```bash
  ./ysdadev <command>
  ```

> [!CAUTION]
> 시스템 전역에 `ysdadev`라는 명령이 설정되어 로컬 래퍼가 쉐도잉(Shadowing)되는 현상을 차단하기 위해, 언제나 점(`.\` 또는 `./`)을 동반한 로컬 래퍼 실행만 공식 지원합니다.

---

## 2. 제공 명사 목록 (Vocabulary & Gate)

| 명령어 (Command) | 기능 및 대상 | 권한 게이팅 (Gated) | 비고 |
|---|---|---|---|
| `list` | 지원 가능한 전체 명령어 명세를 콘솔에 표시합니다. | `Agent-Safe` (자유롭게 실행 가능) | - |
| `doctor` | 로컬 Python, Gradle, adb 실행 상태 및 연결 단말 목록을 체크합니다. | `Agent-Safe` | 환경 진단 시 필수 실행 |
| `build` | 디버그 프로파일로 빌드를 트리거합니다. (`gradlew assembleDebug`) | `Agent-Safe` | `app-debug.apk` 생성 |
| `test unit` | 로컬 JUnit 테스트를 구동합니다. (`gradlew testDebugUnitTest`) | `Agent-Safe` | 테스트 부재 시 성공 완료 |
| `check` | 빌드(`build`)와 단위 검증(`test unit`)을 체인 형태로 연속 구동합니다. | `Agent-Safe` | 커밋 전 정밀 검사 용도 |
| `install` | 빌드된 디버그 APK를 단말에 덮어씁니다. (`adb install -r`) | `Owner-Gated` (명시 승인/지시 필요) | 디바이스 변경 유발 |
| `run` | 단말의 MDRelay MainActivity를 즉각 구동합니다. | `Owner-Gated` | monkey 및 am start 활용 |
| `uninstall` | 기존에 연결된 디바이스에서 com.simpsonys.mdrelay 패키지를 완전히 삭제합니다. | `Owner-Gated` | INSTALL_FAILED_UPDATE_INCOMPATIBLE 등의 시그니처 에러 해결 시 유용 |
| `connect <ip>:<port>` | 지정한 IP 및 포트로 와이어리스 디버깅 디바이스에 연결합니다. | `Owner-Gated` | adb connect 활용 |

---

## 3. 실무 예제

* **로컬 빌드 검증 수행:**
  ```cmd
  .\ysdadev.cmd build
  ```
* **의존 환경 진단:**
  ```cmd
  .\ysdadev.cmd doctor
  ```
* **디바이스 설치 후 앱 구동 (Owner 지시 하에만 실행):**
  ```cmd
  .\ysdadev.cmd install
  .\ysdadev.cmd run
  ```
