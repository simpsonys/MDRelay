# arch/adr-003-web-url-intake-scheme.md

- **Status:** `Accepted`
- **Driver:** YSDA Existing Project Harness Adoption Standard (§E13) & Common Core §C8
- **날짜:** 2026-05-31

---

## 1. Decision Brief (의사결정 브리프)

* **추천 사양:** MDRelay 전용 딥링크 스키마인 `mdrelay://web-open?url={encoded_url}`을 지원하고, 백그라운드 코루틴을 통해 해당 URL의 마크다운/JSON/텍스트 내용을 다운로드하여 인메모리 프리뷰에 자동 적재합니다.
* **추천 이유:** 사용자가 Obsidian, Notion, 혹은 웹 환경에서 마크다운 리소스 Gist 링크를 MDRelay로 유기적으로 보내 열 수 있게 함으로써 릴레이 유틸리티로서의 효용성을 극대화합니다.
* **Owner Action:** 본 Proposed 설계를 검토하고 승인(`Accepted`) 또는 수정 제안을 제공하여 주십시오. 승인 후에 제품 코드 및 매니페스트 변경이 개시됩니다.

---

## 2. Decision Drivers (결정 제약)

* **의존성 극소화 (Zero Dependency):** OkHttp 등 무거운 HTTP 클라이언트를 도입하지 않고, Android SDK 네이티브 `java.net.HttpURLConnection`을 `Dispatchers.IO` 코루틴 상에서 구동하여 앱 크기를 완벽하게 방어해야 합니다.
* **데이터 안전성 및 보안성 (Data Safety & Privacy):** 네트워크 연결을 위해 `android.permission.INTERNET` 권한을 선언하되, 다운로드된 파일은 영구 파일 저장소에 저장하지 않고 인메모리 프리뷰에만 적재하여 유출 위협을 차단합니다.
* **예외 관찰성 (Observability):** 연결 제한(Timeout), 404/500 에러, 빈 응답 또는 잘못된 인코딩 발생 시 크래시를 내지 않고 UI 상에 적합한 토스트/에러 안내를 표출해야 합니다.

---

## 3. 대안 비교 (Alternatives)

| 대안 | 라이브러리 추가 | UX 범용성 | 복잡도 | 리스크 | 비고 |
|---|---|---|---|---|---|
| **안 1. mdrelay://web-open 스키마 + 네이티브 HttpURLConnection** | `없음` (순수 SDK) | `High` (URL 파라미터만 파싱) | `Low` | `Low` | **추천 (가장 경량)** |
| **안 2. http/https 일반 웹 링크 인텐트 전체 가로채기** | `없음` | `Low` (일반 웹 브라우저 실행을 방해하여 사용자 민원 유발) | `Medium` | `High` | 비권장 |
| **안 3. OkHttp + Ktor-client 등의 헤비 네트워크 패키지 도입** | `High` (수 MB 의존성 추가) | `High` | `Medium` | `Medium` | 비권장 (경량 정책 위배) |

---

## 4. Proposed Changes (변경 제안 내용)

### A. Manifest (`app/src/main/AndroidManifest.xml`)
* 인터넷 네트워크 권한 탑재:
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
  ```
* `MainActivity` 내부에 딥링크 스키마용 `<intent-filter>` 선언 추가:
  ```xml
  <intent-filter>
      <action android:name="android.intent.action.VIEW" />
      <category android:name="android.intent.category.DEFAULT" />
      <category android:name="android.intent.category.BROWSABLE" />
      <data android:scheme="mdrelay" android:host="web-open" />
  </intent-filter>
  ```

### B. MainActivity (`MainActivity.kt`)
* `handleIntent`에 딥링크 분석 로직 추가:
  * Intent data가 `mdrelay://web-open`으로 시작하는지 판별.
  * 쿼리 매개변수 `url` 추출 및 URL 디코딩 (`URLDecoder.decode`).
  * 코루틴 스코프 하에서 백그라운드 다운로드 코틀린 순수 로직 구동 (`URL.openStream()`).
  * 다운로드 성공 시 파일명을 URL의 마지막 세그먼트로 추출하여 `incomingFile` 구조체로 변환 및 상태 갱신.
  * 다운로드 실패 시 UI에 Toast 알림 전파.

---

## 5. Verification Plan (검증 방안)

### Automated tests
- `UtilityTest.kt`에 URL 디코딩 및 파일명 추출 유틸리티 단위 테스트 추가.
- `.\ysdadev.cmd check`로 테스트 패스 확인.

### Manual Verification (수동 검증)
- 디스크에 최신 빌드를 올리고 다음 adb 명령어를 통해 deep-link를 구동시켜 Gist 마크다운이 MDRelay 화면에 즉각 로드되는지 확인:
  ```cmd
  adb shell am start -a android.intent.action.VIEW -d "mdrelay://web-open?url=https%3A%2F%2Fgist.githubusercontent.com%2Fsimpsonys%2F7bc1e71327a966e644c5f2a28597f199%2Fraw%2Facbc5de7e430206c85ebfeadcb4d5b736c58b2e7%2FSIMPSONYS_FINANCE_Afternoon_2026-05-31.md" com.simpsonys.mdrelay
  ```
