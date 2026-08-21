# clipnote-android

링크를 공유 카드로 바꿔 주는 서비스 [ClipNote](https://clipnote.co.kr)의 안드로이드 앱.

같은 제품의 다른 얼굴이 셋이다 — 웹·백엔드 [`mjkang0987/clipnote`](https://github.com/mjkang0987/clipnote),
iOS [`mjkang0987/clipnote-ios`](https://github.com/mjkang0987/clipnote-ios), 그리고 여기.
**공유 텍스트 규칙·그라디언트 선택·다국어 문구는 셋이 같아야 한다. 한쪽만 바꾸지 않는다.**

## 문서

| 문서 | 내용 |
|---|---|
| [`index.md`](./index.md) | 프로젝트 구조·현재 상태 |
| [`plan.md`](./plan.md) | 작업 계획·결정 사항·남은 일 |
| [`CLAUDE.md`](./CLAUDE.md) | 작업 규약(브랜치·커밋·검증·위험 명령 금지) |

## 개발

```bash
cp secrets.example.properties secrets.properties   # 값 채우기 (gitignored)
./gradlew :core:test :app:testDebugUnitTest        # 유닛 테스트
./gradlew :app:assembleDebug                       # APK
./gradlew :app:installDebug                        # 기기/에뮬레이터에 설치
```

값을 채우지 않아도 빌드·실행된다 — 예시 파일이 그대로 쓰이고 로그인만 꺼진다.
AdMob 칸의 기본값은 구글 공식 **테스트** ID 다(실 시크릿이 아니다).

## 스택

Kotlin · Jetpack Compose (Material 3) · minSdk 26 / targetSdk 35 · Room · OkHttp ·
kotlinx.serialization · Coil · AdMob · Gradle 8.14 / AGP 8.7.

## 모듈

| 모듈 | 무엇 |
|---|---|
| `:core` | 안드로이드에 기대지 않는 로직 — 모델·API·인증 프로토콜·공유 텍스트·URL 정리·그라디언트 해시·날짜 묶기·딥링크 파싱 |
| `:app` | 화면(Compose)·Room·설정 저장·광고·인텐트 |

**왜 나눴나.** 위 목록은 전부 에뮬레이터 없이 검증할 수 있는 것들이다. 한 모듈에 두면
`:app` 을 조립해야만 돌릴 수 있어서, 로직이 틀렸는지 화면이 틀렸는지 구분이 흐려진다.
`./gradlew :core:test` 는 몇 초에 끝난다.

## 다국어

한국어(원본)·영어·일본어·중국어 간체. **설정 > 표시 언어**에서 바꾸며 재시작이 필요 없다.

문구의 source of truth 는 웹 사전이고, iOS 가 그걸 `Localizable.xcstrings` 로 들고 있다.
안드로이드는 **베끼지 않고 변환한다**:

```bash
python3 scripts/sync-localizations.py ../clipnote-ios/Shared/Localization/Localizable.xcstrings
```

`app/src/main/res/values{,-en,-ja,-zh-rCN}/strings.xml` 이 다시 만들어진다.
**이 파일들을 직접 고치지 않는다** — 다음 동기화에서 덮인다.

기본값(`values/`)이 한국어라, 번역이 빠진 키는 시스템 리소스 폴백이 한국어로 떨어뜨린다.

### 왜 `stringResource()` 를 쓰지 않나

그건 **시스템 언어**를 따른다. 앱 안에서 고른 언어로 그리려면 그 언어의 `Resources` 에서
읽어야 해서, 화면은 `LocalI18n.current.t(R.string.…)` 을 거친다.

## 디자인

토큰·규칙의 source of truth 는 **웹 저장소**(`clipnote/design-guide.md` + `app/globals.css`)다.
iOS 를 보고 옮기지 않는다. 자세한 대조표는 [`index.md`](./index.md) "디자인" 절.

브랜드 서체 **Pretendard**(SIL OFL 1.1)를 번들한다 — 안드로이드 기본 한글 서체는 자소 폭·
굵기 대비가 달라서 같은 문구가 다른 제품처럼 보인다. 라이선스 사본은
`app/src/main/assets/licenses/`.

## 자산

앱 아이콘·브랜드 아이콘·로딩 공룡 스프라이트는 iOS 저장소의 원본에서 만들었다
(`ClipNote/Assets.xcassets`). 공룡은 도트라 리샘플하지 않고 `drawable-nodpi` 에 원본 그대로 둔다.

## 브랜치

`main`(배포) ← `feature/*`. `main` 에 직접 작업하지 않는다.
