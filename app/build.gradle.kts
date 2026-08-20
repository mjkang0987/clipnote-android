import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/// `secrets.properties`(gitignored) 를 읽는다. 없으면 `secrets.example.properties` 로 떨어진다 —
/// CI·새 체크아웃이 값 채우기 전에도 빌드되도록. iOS `Secrets.xcconfig` 와 같은 구성이다.
val secrets = Properties().apply {
    val real = rootProject.file("secrets.properties")
    val example = rootProject.file("secrets.example.properties")
    val source = if (real.exists()) real else example
    if (source.exists()) source.inputStream().use { load(it) }
}

fun secret(key: String, fallback: String = ""): String =
    (secrets.getProperty(key) ?: fallback).trim()

android {
    namespace = "kr.co.clipnote.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.co.clipnote.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE", "\"${secret("API_BASE", "https://clipnote.co.kr")}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${secret("NAVER_CLIENT_ID")}\"")
        buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"${secret("ADMOB_BANNER_UNIT_ID")}\"")

        // AdMob App ID 는 매니페스트 meta-data 로만 들어간다. **빈 값이면 SDK 가 앱을 죽인다** —
        // iOS 1.1.0 이 실행 즉시 크래시한 원인이 이 칸이었다(광고 단위 ID 를 넣어 두었다).
        // 예시 파일의 기본값은 구글 공식 테스트 App ID 라, 값을 안 채워도 앱은 산다.
        manifestPlaceholders["admobAppId"] =
            secret("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
    }

    signingConfigs {
        // 릴리스 서명은 CI 시크릿으로 주입한다. 키스토어가 없으면 설정 자체를 만들지 않아
        // `assembleDebug` 가 영향을 받지 않게 둔다.
        val storeFilePath = secret("RELEASE_STORE_FILE")
        if (storeFilePath.isNotEmpty() && rootProject.file(storeFilePath).exists()) {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // 개발 빌드는 구글 공식 테스트 배너 unit 을 쓴다 — 실광고 자가 클릭은 계정 정지 사유다.
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}
