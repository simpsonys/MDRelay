# workflow/ROLES.md

## YSDA 프로젝트 역할 매핑 (MDRelay)

프로젝트 안정성과 체계적인 빌드/운영 관리를 위해 각 에이전트 동작은 다음 역할 모델에 정합됩니다.

### 1. Owner (사용자)
* **책임:** 최종 의사결정권자.
* **주요 권한:**
  * Proposed 상태의 ADR에 대한 최종 승인(`Accepted`/`Rejected`).
  * `ysdadev` 상에서 게이트된(owner-gated) 명령의 실행 지시 또는 수락(`install`, `run`, `release` 등).
  * 원격 저장소(`origin/main`)에 대한 `git push` 행위 권장 및 실행.

### 2. Planner (기획자 / 에이전트)
* **책임:** 요구사항 및 마일스톤 관리.
* **생산 산출물:** `workflow/STATUS.md`, `doc/current-state.md`

### 3. Architect (아키텍트 / 에이전트)
* **책임:** 설계 정의 및 기술 제약 준수.
* **생산 산출물:** `arch/adr-*.md` (Proposed), `arch/current-architecture.md`
* **제약:** Owner의 승인 없이 Proposed ADR을 Accepted로 독단 변경할 수 없음.

### 4. Implementer (개발자 / 에이전트)
* **책임:** 소스 코드 구현 및 로컬 유틸리티 준비.
* **생산 산출물:** `scripts/ysdadev.py`, `ysdadev.cmd` 및 실제 제품 구현 코드.
* **제약:** Accepted 상태의 설계와 `quality-bar.md` 제약을 침범하지 않는 선에서만 구현 가능.

### 5. Evaluator/QA (검증자 / 에이전트)
* **책임:** 품질 점검 및 테스트 수행.
* **생산 산출물:** `qa/manual-test-checklist.md`, `reports/progress.md`
