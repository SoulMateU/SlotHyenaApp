plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

android {
    namespace = "kr.slot.hyena"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    defaultConfig {
        applicationId = "kr.slot.hyena"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"
    }
}

// 원본 APK는 Gradle이 증분 빌드 추적에 사용하므로 남겨두고,
// 설치·배포용 파일만 별도 이름으로 복사한다.
tasks.configureEach {
    if (name == "assembleDebug") {
        doLast {
            val apkDirectory = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
            val originalApk = apkDirectory.resolve("app-debug.apk")
            val namedApk = apkDirectory.resolve("SlotHyena-debug.apk")
            if (originalApk.exists()) {
                originalApk.copyTo(namedApk, overwrite = true)
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
}
