# arch/adr-000-existing-architecture-baseline.md

- **Status:** `Accepted`
- **Driver:** YSDA Existing Project Harness Adoption Standard (§E5)
- **날짜:** 2026-05-31

---

## 1. Context & Rationale (맥락 및 설명)

MDRelay는 이미 특정 요구사항을 충족하도록 제작 및 운영되고 있는 경량 Android 애플리케이션입니다. 본 ADR은 신규 기술적 아키텍처 제안이 아닌, **현재 프로젝트가 채택하고 있는 기술적 아키텍처 베이스라인**을 기록하여 향후 에이전트와 오너가 일관성 있는 방향으로 협업할 수 있도록 돕습니다.

MDRelay의 핵심 철학은 **"InkNote가 아니다. 빠른 시작, 경량 뷰어, 안전한 릴레이 기능만 제공한다"**입니다. 이에 맞추어 모든 부수적 요소는 고도로 절제되어 설계되었습니다.

---

## 2. Technical Decisions (기술적 결정 사항)

### A. UI 및 언어 스택
* **선택:** Kotlin 2.0.0 + Jetpack Compose + Compose compiler 2.0.0
* **이유:** 선언형 컴포즈 UI를 채택하여 불필요한 레이아웃 XML 파일 누적을 제거하고, 상태 기반의 렌더링으로 뷰 성능을 최대화함.

### B. 단일 화면 아키텍처 (Single Activity Structure)
* **선택:** `com.simpsonys.mdrelay.MainActivity` 단일 진입점 구현.
* **이유:** 앱의 상태(Viewer 모드, 임시 Edit 모드)를 단일 액티비티 컨텍스트 내에서 제어하여 복잡한 라우팅 라이브러리 도입 비용을 없애고 즉시 로드(Fast Launch)를 달성함.

### C. 데이터 저장 및 지속성 배제 (No Local Database / Sync)
* **선택:** SQLite, Room Database, Wikistructure, 혹은 Folder/Tag 관리 데이터베이스를 **일절 갖추지 않음**.
* **이유:** 임시 수정된 문서 릴레이 목적이므로, 복잡한 로컬 DB 관리와 싱크 충돌 위험을 원천 차단함. 지속 보관은 OS 파일 스토리지나 외부 연동에 전가함.

### D. 패키지 가시성 및 FolderSync 쿼리 지정
* **선택:** `AndroidManifest.xml` 내에 `dk.tacit.android.foldersync.full` 및 `lite` 명시 선언.
* **이유:** 외부 동기화 솔루션인 FolderSync와의 정상적인 통신과 패키지 존재 확인을 Android 11+ 가시성 제약 하에서 보장하기 위함.

### E. 스토리지 권한 제약 (No Broad Storage Permission)
* **선택:** 디바이스 전역 파일 읽기/쓰기 권한 거부 및 Android Intent URI 임시 권한 전유.
* **이유:** 타 프라이버시 파일 유출 가능성을 원천 차단하며 Android Sandbox 보안 가이드라인에 완전히 일치시킴.

### F. 폰트 정책
* **선택:** 디바이스 시스템 기본 폰트(Samsung Default, Android System Font) 백퍼센트 반영.
* **이유:** custom 폰트 리소스 적재로 인한 APK 용량 증가를 막고, 단말별 사용자 접근성 스케일과 단말 선호 폰트를 그대로 렌더링에 반영하기 위함. Monospace 폰트만 코드 블록 용도로 내장 Monospace 상수를 활용함.

---

## 3. Implications & Blast Radius (영향 및 범위)

1. **초경량 유지:** 추가적인 데이터베이스 마이그레이션이나 복잡한 폰트/테마 엔진 관리가 불필요합니다.
2. **코드 수정 제약:** 뷰어 개선은 허용되나, 지속성(Save back), 라이브러리 추가, 전체 WebView 변환 등의 행위는 이 아키텍처 베이스라인을 침범하므로 강력하게 금지됩니다.
3. **디버깅 방향:** 오류 감지 및 보고 시, UI 수준의 가시적인 경고 전파와 `Logcat`을 통한 Stacktrace 출력을 활용합니다.
