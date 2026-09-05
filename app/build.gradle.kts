plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

import java.util.Properties

// Подпись релиза берётся из keystore.properties в корне проекта (файл НЕ в git).
// Формат:
//   storeFile=/home/nighty/sibsutis-release.jks
//   storePassword=***
//   keyAlias=sibsutis
//   keyPassword=***
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) load(file.inputStream())
}
val hasReleaseKey = keystoreProperties.containsKey("storeFile")

android {
    namespace = "app.vercel.Nighty3098.schedule"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "app.vercel.Nighty3098.schedule"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKey) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // Подписываем, только если создан keystore.properties (см. верх файла).
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material.icons.ext)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Сеть и парсинг: OkHttp + Jsoup
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.jsoup)

    // Локальное хранилище: Room (кэш) + DataStore (настройки)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Шифрованное хранение логина/пароля (Android Keystore + AES256)
    implementation(libs.androidx.security.crypto)

    // Виджет: Jetpack Glance
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Фоновое обновление виджета/кэша
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    // org.json есть только в Android-фреймворке; для JVM-тестов парсера берём Maven-версию.
    testImplementation(libs.json.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}