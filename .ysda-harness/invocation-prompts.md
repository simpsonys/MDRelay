# YSDA Harness Invocation Prompts (Appendix A Mirror)

이 파일은 `ysda-existing-project-harness-adoption-standard.md` Appendix A의 프롬프트를 미러링하여 보관합니다. 에이전트와 대화를 재개하거나 지시를 내릴 때 아래 프롬프트를 복사하여 사용할 수 있습니다.

## A.1 Adopt — short default (기본 도입 프롬프트)
```text
이 기존 프로젝트를 현재 YSDA Harness 기준으로 점검하고 필요한 부분만 보강해줘.
제품 코드 변경 없이 workflow, .ysda-harness, .gitignore, owner-facing ysdadev façade, 필요한 경우 CI/CD artifact retention 정책을 프로젝트 성격에 맞게 준비해줘.
결과는 한국어로 compact하게 요약해줘.
```

## A.2 Adopt — strict / audit mode (엄격 점검/오딧 모드 프롬프트)
```text
이 기존 프로젝트를 현재 YSDA Harness 기준으로 점검하고 필요한 부분만 보강해줘. 제품 코드 수정 금지.
현재 구조를 먼저 이해(§E5/E12)하고, §E7 11개 렌즈로 숨은 결정 발굴 → adr-candidates(최대 5, 기본 Blocking=No).
기존 devtool은 다시 짜지 말고 ysdadev로 매핑(§E11). CI/CD/릴리즈 산출물이 있으면 retention/release 정책은 Common §C29에 맞춰 점검. global ysdadev에 의존하지 말고 project-local wrapper만 사용.
legacy debt는 active blocker가 아니면 Checkpoint(§E9). dependency 추가·migration·push 금지.
결과는 한국어로 compact하게(수정 파일 / 현재 상태 / 발견 문제 / ADR 후보 / Checkpoints / 다음 액션).
```

## A.3 Scoped analysis (large / company slice) (스코프 단위 부분 분석 프롬프트)
```text
거대 코드베이스의 일부분을 분석한다. host repo는 수정·재구성·push 금지. 결과는 analysis/<slice>/ sidecar로만.
1) scope.md 먼저: 질문, in/out-of-scope, "읽지 않을 것" — monorepo 전체 스캔 금지, in-scope 의존 그래프만.
2) read-only. 기밀: host 코드 외부 반출 금지, 산출물 redact(Common §C20).
3) §E7 렌즈 + 경계 렌즈(contract/ownership/가정/blast radius)로 발굴.
4) 산출물(self-contained): scope / current-architecture(slice) / adr-candidates(≤5) / risk-register(≤10) / open-questions / qna-log.
고치지 말고 분석·핸드오프. 한국어 compact 요약.
```
