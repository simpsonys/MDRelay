# doc/current-state.md

## MDRelay 현황 및 조사 보고서 (Current State)

* **조사 일자:** 2026-05-31
* **분석 대상:** MDRelay Android Application (`app/` 및 프로젝트 구성 파일)

---

## 1. 프로젝트 주요 현황 요약
* **프로젝트 성격:** AI generated 마크다운(`.md`), 플레인 텍스트(`.txt`), JSON(`.json`) 문서용 초경량 뷰어 겸 릴레이 앱.
* **타겟 플랫폼:** Android compileSdk 35 / minSdk 26
* **개발 언어:** Kotlin 2.0.0
* **UI 프레임워크:** Jetpack Compose (Compose Compiler 2.0.0 내장)
* **의존성(Dependencies):** `androidx.core:core-ktx`, `androidx.activity:activity-compose`, Compose Foundation 및 Material3.
* **빌드 도구:** Gradle v8.5.0
* **핵심 진입점:** `com.simpsonys.mdrelay.MainActivity` (`MainActivity.kt` 단일 파일로 뷰어와 모든 라이프사이클 처리)

---

## 2. 외부 인입 인터페이스 분석 (Intent & Filters)

`app/src/main/AndroidManifest.xml` 분석에 의거한 인입 사양은 다음과 같습니다.

### A. Intent VIEW (파일 직접 열기)
1. **MimeType 기반:**
   - `text/markdown`, `text/plain`, `application/json` 타입 수신 가능.
   - `content://` 및 `file://` 스키마 지원.
2. **확장자 패턴 기반:**
   - `.md`, `.markdown`, `.txt`, `.json` 파일 경로 패턴 매칭 지원.
3. **Octet-stream 처리:**
   - 다운로드나 브라우저 등에서 알 수 없는 바이너리 형태로 전달될 때, 확장자가 `.md`, `.markdown`, `.txt`, `.json`에 해당하는 경우 강제 파싱 및 기동 허용.

### B. Intent SEND (공유 텍스트/파일 받기)
- 타 앱에서 공유하기를 통해 `text/markdown`, `text/plain`, `application/json`, `text/uri-list` 및 `application/octet-stream`을 MDRelay로 유기적으로 전달 가능.

---

## 3. 데이터 안전성 및 스토리지 접근 정책 (Data Safety)

### A. Broad Storage Permission 배제
- `AndroidManifest.xml` 상에서 넓은 영역의 파일 읽기/쓰기 권한(`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`)을 **일절 선언하지 않음**.
- Android `content://` 스키마 URI를 안전하게 전달받아 한시적 읽기 권한(`FLAG_GRANT_READ_URI_PERMISSION`) 범위 안에서만 파일을 렌더링함.
- **안전성 효과:** 디바이스 내 타 개인정보 파일 유출 위협이 근본 차단됨.

### B. 임시 편집 모드(Relay-time edit)
- 외부 문서 파일을 읽어온 뒤 로컬 캐시 메모리에만 텍스트를 담아 임시 수정을 허용함.
- 수동 저장(Save)이 아닌 이상, 원본 파일 위치로의 즉각적 덮어쓰기(Writeback)를 수행하지 않으므로 오작동 시의 원본 유실 위협을 무력화함.

---

## 4. 외부 동기화 연동 사양 (FolderSync Integration)

### A. Package Query 지정
- Android 11(API 30)부터 적용된 패키지 가시성 제약(Package Visibility)을 충족하기 위해 `<queries>` 필드 내에 다음 두 패키지를 명시적으로 선언함:
  - `dk.tacit.android.foldersync.full` (FolderSync Full 버전)
  - `dk.tacit.android.foldersync.lite` (FolderSync Lite 버전)
- **목적:** FolderSync 백그라운드 동기화 툴이 지정한 로컬 마크다운 폴더 내에 신규 생성된 파일이 있음을 감지하고, MDRelay가 해당 패키지 상태나 단말기 인입 데이터를 안전하게 판독할 수 있도록 보장함.

---

## 5. SAF Outbox 연동
- 임시 수정 및 뷰어에 적재된 콘텐츠는 Android SAF(Storage Access Framework)를 통해 수동 내보내기/공유를 수행하므로 시스템 수준의 샌드박스 보안 규칙을 엄격하게 준수함.
