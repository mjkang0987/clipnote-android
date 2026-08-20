# ClipNote Android — index.md

> 프로젝트 구조와 현재 상태의 source of truth. 작업 완료 시 갱신한다.

## 프로젝트 정보
- **이름**: ClipNote (안드로이드 앱)
- **applicationId**: `kr.co.clipnote.app` (URL 스킴 `clipnote://`) — iOS 번들 ID 와 같은 값이라
  Supabase 리다이렉트·네이버 콜백 설정을 공유한다
- **스택**: Kotlin · Jetpack Compose(Material 3) · minSdk 26 / targetSdk 35 · Gradle 8.14 / AGP 8.7
- **백엔드**: `API_BASE`(clipnote.co.kr) · Supabase 인증 · 네이버 로그인 · AdMob
  (설정은 `secrets.properties`)
- **배포**: 아직 파이프라인 없음 — `plan.md` "남은 일" 참고

## 빌드
```bash
cp secrets.example.properties secrets.properties   # 값 채우기 (gitignored)
./gradlew :core:test :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## 구조

### `:core` — 안드로이드에 기대지 않는 로직

| 경로 | 역할 |
|------|------|
| `model/Models.kt` | 도메인 모델(`ClipMetadata`·`DbClip`·`CreateClipInput`·`UClip`·`AuthProvider`) |
| `net/ApiClient.kt` | clipnote.co.kr API 7개 엔드포인트(메타·클립 생성·OG·목록·수정·삭제·계정삭제) |
| `net/Json.kt` | 공용 JSON 설정. `explicitNulls = false` — null 필드를 **보내지 않는다** |
| `util/Text.kt` | `buildShareText`(제목\nURL)·`truncateShareTitle`(80자)·`parseTags`(최대 6)·`orderedUnique` |
| `util/Urls.kt` | `isFetchableUrl`·`prettyHost`·`proxiedImageUrl`(`/api/image?url=`)·`openableWebUrl` |
| `util/Iso8601.kt` | Supabase `timestamptz`(마이크로초) 파싱 |
| `theme/Gradients.kt` | 8색 팔레트 + `pickGradient`(웹 JS 해시와 **같은 결과**) |
| `clips/ClipGrouping.kt` | `DbClip → UClip` 매핑 + 날짜 묶기(`ClipDateBucket`) |
| `auth/DeepLinks.kt` | `clipnote://auth/...`·`clipnote://share?url=` 파싱 |
| `auth/Pkce.kt` | PKCE verifier/challenge, 네이버 nonce |
| `auth/SupabaseAuth.kt` | GoTrue REST(authorize URL·code 교환·magiclink verify·refresh·logout) |
| `auth/NaverAuth.kt` | 네이버 authorize URL(서버 콜백 → magiclink) |
| `auth/Session.kt` | 세션·계정 정보·로그인 실패 케이스 |
| `core/src/test/` | 유닛 테스트 49개 |

### `:app` — 화면과 안드로이드에 붙는 것들

| 경로 | 역할 |
|------|------|
| `MainActivity.kt` | 유일한 Activity(`singleTask`). 인텐트 라우팅·NavHost·로그아웃/옮기기 레이어 |
| `AppContainer.kt` | 오래 사는 것들을 한 자리에(prefs·api·db·i18n·auth). DI 프레임워크 없음 |
| `ClipNoteApplication.kt` | 컨테이너 생성 + AdMob 초기화(설정 있을 때만) |
| `ClipsRefresh.kt` | 목록 새로고침 신호(SharedFlow) |
| `data/AppPrefs.kt` | 온보딩·언어·세션·PKCE·태그 빈도 |
| `data/LocalClip*.kt`, `AppDatabase.kt` | Room — 이 기기 클립(url 기본키·최대 300·최신순) |
| `i18n/AppLanguage.kt` | 지원 언어(ko·en·ja·zh-Hans) + 시스템 언어 매칭 |
| `i18n/LocalizationStore.kt` | 표시 언어 상태 + 언어별 `Resources` 조회 → **재시작 없이 전환** |
| `auth/AuthStore.kt` | 세션의 단일 소유자. 만료 전 갱신(뮤텍스로 한 번만) |
| `ads/AdConfig.kt`, `AdBanner.kt` | 앵커 적응형 배너. 설정 없으면 광고만 끄고 앱은 산다 |
| `ui/theme/Theme.kt` | 팔레트·모서리·그라디언트 브러시 |
| `ui/Navigation.kt`, `Locals.kt` | 라우트·`AppRouter`·Custom Tabs·CompositionLocal |
| `ui/components/` | `AppScaffold`(상단 메뉴+배너)·버튼·카드·썸네일·칩·`ConfirmLayer`·공룡 |
| `ui/home/` | 홈 화면 + `HomeViewModel`(600ms 디바운스 메타) + 공유 결과 시트 |
| `ui/clips/` | 목록(`ClipsViewModel`·`ClipsScreen`)·이 기기 클립 화면·편집/태그 시트·옮기기 |
| `ui/settings/` | 설정(표시 언어·계정·문의·탈퇴 진입) |
| `ui/login/LoginSheet.kt` | 로그인(Google/Kakao/Naver) + 동의 + 게스트 |
| `ui/info/` | 소개·FAQ·개인정보처리방침(정적)·회원 탈퇴 |
| `ui/onboarding/` | 실제 홈 위에 얹는 스포트라이트 투어 |
| `app/src/test/` | 유닛 테스트 8개(공유 인텐트 URL 추출·언어 매칭) |

### 리소스
- `res/values{,-en,-ja,-zh-rCN}/strings.xml` — **자동 생성.** 직접 고치지 않는다
  (`scripts/sync-localizations.py`).
- `res/drawable-nodpi/dino_run{1..4}.png` — 도트 스프라이트. 밀도 스케일을 막으려고 `nodpi`.
- `res/mipmap-*/ic_launcher*.png` — iOS 앱 아이콘 1024 원본에서 생성.

## 현재 상태

- **iOS 기능 패리티 1차 완료** — 홈(메타 추출·미리보기·저장·공유 링크)·내 클립(필터·편집·삭제·
  다중선택·태그 일괄)·이 기기 클립·로그인 3종·설정·소개/FAQ/개인정보/탈퇴·온보딩 투어·다국어 4개·
  AdMob 배너·공유 인텐트 수신·딥링크.
- **검증**: `:core` 유닛 테스트 **49개 그린**(이 컨테이너에서 실행 확인).
  `:app` 은 **컴파일·실행을 아직 확인하지 못했다** — 이 작업 환경에서 Android SDK
  (`dl.google.com`)를 받을 수 없다. 첫 CI 실행이 실질적인 첫 컴파일이다.
  자세한 내용은 `plan.md` "검증 상태".
- **AdMob**: 실 ID 는 아직 없다. `secrets.example.properties` 의 구글 테스트 ID 로 동작한다.
- **배포**: Play 콘솔·서명 키·업로드 파이프라인 없음.

## iOS 와 일부러 다르게 한 것

| 항목 | iOS | 안드로이드 | 이유 |
|---|---|---|---|
| 공유 받기 | 별도 확장 타깃 + App Group | 같은 Activity 의 `ACTION_SEND` | 확장이 없으니 언어 설정을 공유할 문제도 없다 |
| Supabase | `supabase-swift` SDK | GoTrue REST 직접 호출 | 쓰는 표면이 넷뿐이고, REST 로 두면 `:core` 에서 테스트된다 |
| 날짜 머리글 | 모델이 문자열 생성 | 모델은 케이스, 화면이 ICU 로 그림 | 표시 언어가 바뀌면 머리글도 따라와야 한다 |
| 헤더 메뉴 | 사이드 슬라이드 | 상단 오버플로 메뉴 | 플랫폼 관례 |
| 모듈 | 단일 타깃 | `:core` + `:app` | 로직을 에뮬레이터 없이 검증하려고 |

## 설정 파일
- `secrets.example.properties` — 시크릿 템플릿(실제 `secrets.properties` 는 gitignored)
- `gradle/libs.versions.toml` — 버전 카탈로그
- `.github/workflows/ci.yml` — CI(ubuntu): 유닛 테스트 → 디버그 조립 → lint
