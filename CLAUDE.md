# CLAUDE.md

> 이 저장소에서 Claude가 항상 따라야 할 지시사항. 세션 시작 시 `index.md`·`plan.md`와 함께 읽는다.

## Priority Order
1. Core Principles
2. Session Startup Rules
3. Development Workflow
4. Work Request Flow
5. Android/Kotlin Standards
6. Documentation Maintenance

## Core Principles
- If unsure, say so instead of guessing.
- Point out problems with my approach directly.
- If something fails, investigate the root cause before retrying.

## Session Startup Rules
- 새 세션 시작 시 `index.md`·`plan.md`를 먼저 읽는다.
- `index.md`는 프로젝트 구조·현재 상태의 source of truth, `plan.md`는 현재/향후 작업의 source of truth.
- 문서와 구현이 다르면 불일치를 보고하고 확인받은 뒤 진행한다.
- **세 저장소(웹·iOS·안드로이드)가 공유하는 규칙**을 건드릴 때는 나머지 둘도 함께 본다 —
  공유 텍스트 형식, 그라디언트 해시, 다국어 키, API 계약.

## Development Workflow
- **작업 계획 수립:** 모든 작업을 시작하기 전 `plan.md`를 작성할 것. 요구사항, 구현 방식, 영향받는 파일,
  예상 결과를 기록하고 검토가 끝난 후 코드를 수정할 것.
- **작업 분할 및 브랜치 생성:** 작업 요청 시 가장 작은 단위의 이슈로 나누고, **`main` 최신본을 기준으로**
  개별 `feature` 브랜치를 생성하여 시작할 것.
- **Feature 검증 사이클:** `작업` > `코드리뷰` > `개선` > `검증` > `수정작업` > `코드리뷰` > `개선` > `검증`
  — 이 프로세스를 `feature` 브랜치 내에서 **완벽히 완료**할 것.
- **Main 재동기화 (필수):** PR 직전 `origin/main`을 병합하고 **검증을 다시 통과**시킬 것.
- **Main 배포:** feature 브랜치에서 `main`으로 PR을 생성하고 머지를 **요청**할 것.
  지시자의 명시적 승인 없이 `main`에 머지하지 않는다.
- **버전 펌핑:** PR 머지 시 변경 규모(Patch / Minor / Major)를 판단하여 올릴 것.
  `app/build.gradle.kts` 의 `versionName`·`versionCode` 두 곳을 함께 고친다.
  `versionCode` 는 Play 콘솔에 올린 값보다 커야 한다 — 되돌릴 수 없다.

## Work Request Flow (업무 처리 절차)

**세부 규약:**
- **이슈당 브랜치 · 이슈당 PR.** 브랜치명 `feature/<짧은슬러그>`(또는 `claude/issue-<번호>-<슬러그>`),
  **`main`에서 분기 · `main`으로 PR**. 한 번에 한 이슈.
- **PR 생성까지만 자동 진행.** 검증·리뷰·CI 가 그린이면 PR 을 열어 두고 보고한다.
- **라벨**: `feature`/`fix`/`chore`/`refactor`/`docs`(없으면 생성).
- **검증 범위**: 항상 `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug`.
  로직이 `:core` 에 있으면 그쪽 테스트를 먼저 추가한다.

1. **업무 요청 접수** — 모호하면 먼저 질문해 범위를 확정한다(추측 금지).
2. **이슈 분할·생성** — 작업 단위로 GitHub 이슈 생성(배경·작업 체크리스트·완료 조건·관련 파일).
3. **작업** — `main`에서 이슈당 브랜치를 만들어 구현. 커밋은 최소 단위·한국어·conventional prefix.
4. **검증** — 유닛 테스트 + 디버그 조립 + lint.
5. **코드리뷰** — `/code-review`로 diff 리뷰.
6. **재검증** — 리팩토링 후 다시 빌드/테스트. **Main 재동기화** 후 4~6 반복.
7. **PR 생성** — base 는 **`main`**, 본문에 `Closes #<이슈>`. CI(`.github/workflows/ci.yml`)가 돈다.
8. **머지** — 그린이고 **지시자 승인이 있으면** `main` 으로 머지. `index.md`·`plan.md` 갱신.

## Android/Kotlin Standards
- **컴포넌트 재사용 우선:** 기존 컴포저블 재사용을 최우선 기준으로 삼을 것.
- **신규 컴포넌트 생성 통제:** 불가피할 경우 코드 작성 전에 '새로 만들어야 하는 이유'를 브리핑하고
  승인을 받은 후 진행할 것.
- **널 안전**: `!!` 지양. `?.`/`?:`/스마트 캐스트를 쓴다.
- **코루틴**: 화면 수명을 넘는 작업은 `viewModelScope`·`AppContainer.scope` 에 붙인다.
  `CancellationException` 은 삼키지 않고 다시 던진다.
- **로직은 `:core` 에**: 안드로이드 타입(`Context`·`Uri`·`Resources`)에 기대지 않는 로직은 `:core` 로
  옮겨 유닛 테스트를 붙인다. `android.net.Uri` 대신 `java.net.URI` 를 쓰는 이유가 그거다.
- **문자열**: 화면 문구는 반드시 `LocalI18n.current.t(R.string.…)`. `stringResource()` 는 시스템
  언어를 따라서, 앱 안에서 고른 언어를 무시한다.
- **오류는 문자열이 아니라 케이스로**: 모델이 문장을 만들면 그 시점 언어로 굳는다.
- **보안**: 시크릿을 코드에 하드코딩하지 않는다(`secrets.properties`). 로그에 민감정보 금지.
- 접근성(글자 크기 배율·`contentDescription`) 고려.

## Documentation Maintenance
- 작업 완료 후 `index.md`·`plan.md`를 갱신한다.

## On Commit
- 커밋은 최소 단위로 나눈다. 한국어. conventional prefix(`feat:`/`fix:`/`refactor:`/`chore:` 등).
  커밋 후 항상 push.

## 위험한 명령 금지 (사고 재발 방지)

되돌릴 수 없는 작업으로 실제 데이터를 잃은 사고가 있었다(GitHub Secret 덮어쓰기, 그 이전 DB 삭제).
아래는 예외 없이 지킨다.

- **되돌릴 수 없는 명령은 제안하지 않는다.** 덮어쓰기·삭제·원격 반영은 명령 대신 **UI 경로로 안내**한다.
  (시크릿 갱신, force push, DB 마이그레이션·삭제, `rm`, 기존 파일을 덮는 `cp`/`>` 등)
- 명령이 불가피하면 **무엇이 사라지는지 먼저 적고, 승인을 받은 뒤** 제시한다.
- **읽기 명령과 쓰기 명령을 한 묶음으로 주지 않는다.**
- 기존 값이 있는 대상은 **현재 상태를 먼저 확인**하는 단계를 둔다.
- 값을 다시 읽을 수 없는 저장소(GitHub Secrets·Play 업로드 키 등)는 특히 주의한다 —
  버전 이력도 백업도 없다. **업로드 키를 잃으면 그 앱을 다시 올릴 수 없다.**
