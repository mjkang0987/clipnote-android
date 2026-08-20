// 순수 JVM 모듈 — Android SDK 에 기대지 않는 로직만 둔다.
//
// **왜 나눴나.** 모델·API·공유 텍스트·URL 정리·그라디언트 해시·날짜 그룹·딥링크 파싱은
// 안드로이드가 필요 없다. 여기 두면 에뮬레이터 없이 `./gradlew :core:test` 로 도는 유닛
// 테스트가 되고(iOS 의 `ClipNoteTests` 대응), 화면 코드가 로직을 붙들고 있지 않게 된다.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    api(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
