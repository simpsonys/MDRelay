# qa/manual-test-checklist.md

## MDRelay 수동 검증 체크리스트 v2.8.5

이 체크리스트는 빌드 완료 후 연결된 Android 디바이스(또는 에뮬레이터)에서 MDRelay의 주요 기능을 유기적으로 검증하기 위한 매뉴얼 가이드입니다. 

검증 시 공식 로컬 Façade인 `.\ysdadev.cmd` 및 `adb` 명령어를 병용합니다.

---

### §1. 앱 기동 및 단독 기동 검증 (Fast Launch & Base UI)
* **목표:** 앱이 지연 없이 빠르게 기동되고 기본 화면이 시스템 폰트를 따르는지 확인합니다.
* **수행 단계:**
  1. `.\ysdadev.cmd build` 로 APK가 최신 빌드 상태인지 확인.
  2. `.\ysdadev.cmd install` 로 디바이스에 앱 설치.
  3. `.\ysdadev.cmd run` 으로 앱 실행.
* **통과 조건:**
  - [ ] 앱 런처 아이콘 클릭 또는 명령어 실행 후 즉각 런칭되는가 (Fast Launch).
  - [ ] 화면 본문 폰트가 Samsung/Galaxy/Google 등 기기 시스템 폰트 형태를 그대로 유지하는가 (Font Policy 준수).
  - [ ] Monospace 폰트가 인라인 코드나 코드 블록에만 선별 적용되었는가.

---

### §2. Intent VIEW 수신 검증 (외부 파일 열기)
* **목표:** 외부 다운로드 디렉토리나 앱에서 `.md`, `.txt`, `.json` 파일을 열었을 때 MDRelay가 구동되어 내용이 정상 뷰어로 출력되는지 검증합니다.
* **수행 단계 (ADB 활용):**
  1. PC의 가상 샘플 파일 `sample.md` 생성 후 디바이스 다운로드 폴더로 밀어 넣기:
     ```cmd
     adb push sample.md /sdcard/Download/sample.md
     ```
  2. adb shell의 Intent VIEW 명령어로 MDRelay 호출:
     ```cmd
     adb shell am start -a android.intent.action.VIEW -d "file:///sdcard/Download/sample.md" -t "text/markdown" com.simpsonys.mdrelay
     ```
* **통과 조건:**
  - [ ] 파일 뷰어가 즉각 구동되고 마크다운 마크업이 깔끔하게 렌더링되는가.
  - [ ] 빈 파일이나 존재하지 않는 경로를 호출했을 때, 크래시가 아닌 사용자 예외 에러 카드나 토스트 메시지가 나타나는가 (Observability).

---

### §3. Intent SEND 수신 검증 (텍스트/파일 공유 받기)
* **목표:** 타 앱(인터넷 브라우저, 메모장 등)에서 텍스트 혹은 파일을 "공유하기"로 보냈을 때 MDRelay가 이를 인입하여 뷰어에 표시하는지 확인합니다.
* **수행 단계:**
  1. 타 앱에서 마크다운 텍스트를 드래그한 후 '공유' 버튼 클릭 -> 수신 앱 목록에서 'MDRelay' 선택.
  2. 또는 ADB를 통한 모의 SEND 전달:
     ```cmd
     adb shell am start -a android.intent.action.SEND --es "android.intent.extra.TEXT" "# Test Title\n\nThis is shared content" -t "text/plain" com.simpsonys.mdrelay
     ```
* **통과 조건:**
  - [ ] 텍스트 원본이 깨짐 없이 MDRelay 메인 뷰어 상에 즉각 적재되어 프리뷰 모드로 전환되는가.

---

### §4. 임시 편집(Relay-time edit) 및 클립보드 복사/공유 내보내기 검증
* **목표:** 수신한 마크다운을 간단히 임시 편집하고 이를 다시 외부로 복사/공유하는 릴레이 흐름이 잘 보장되는지 확인합니다.
* **수행 단계:**
  1. 외부 문서 인입 상태에서 우상단 '임시 편집(Edit)' 혹은 관련 버튼 탭.
  2. 임의의 오탈자 한 글자 수정.
  3. '클립보드 복사' 혹은 '공유하기' 버튼 선택.
  4. 복사된 내용을 다른 메모장에 붙여넣기(Paste).
* **통과 조건:**
  - [ ] 임시 편집 모드가 부드럽게 활성화되고 타이핑이 가능한가.
  - [ ] 공유/복사된 결과물에 편집된 내용이 올바르게 반영되었는가.
  - [ ] **Data Safety:** 이 모든 수정 과정에서 기기 저장소의 원래 원본 파일(`.md`)이 무단 덮어쓰기(Writeback)되지 않았음을 확인.

---

### §5. FolderSync 및 SAF 가상 연동 검증
* **목표:** FolderSync 백그라운드 싱크로 로컬 폴더에 인입된 문서를 불러올 때 생기는 URI 권한 및 SAF 아웃박스 동작을 검증합니다.
* **통과 조건:**
  - [ ] 뷰어 실행 중에 FolderSync로 유입된 `content://` 스키마 URI의 접근이 차단당하지 않고 자연스럽게 읽혀 렌더링되는가.
  - [ ] 뷰어에 띄워진 문서를 SAF(Storage Access Framework)를 거쳐 다른 커스텀 outbox 경로에 저장할 때 정상 수행되는가.
