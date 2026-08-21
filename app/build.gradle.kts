import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)

}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.gps.warehouse"
    compileSdk {
        version = release(36)
    }

    /**
     * Обязательно отредактировать файл com.gps.warehouse.utils.Constants.kt
     */
    defaultConfig {
        applicationId = "com.gps.warehouse"
        minSdk = 30
        targetSdk = 36
        versionCode = 16        // 16 (текущая на сервере) -> 17 // - Обновление приложения происходит по этому параметру!
//        versionName = "1.1.6"   // TEST v1.1.6 (текущая) -> v1.1.7
        versionName = "1.0.3"   // PROD v1.0.3 (текущая) -> v1.0.4

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("my_debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "android"
            keyPassword = "android"
        }
        create("my_release") {
            storeFile = file("release.keystore")
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("my_debug")
            isMinifyEnabled = false
        }
        release {
            // Для релиза используем отдельный release.keystore!
            signingConfig = signingConfigs.getByName("my_release")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    buildToolsVersion = "36.0.0"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Подключение локальной библиотеки для Сканера Honeywell
    implementation(files("libs/DataCollection.aar"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.car.ui.lib)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // Unit Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

    // Test UI
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt (DI) - опционально, можно использовать Koin или ручную DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Для Превью
//    androidTestImplementation(libs.hilt.android.testing)
//    ksp(libs.hilt.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit2.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // JSON - TEXT
    implementation(libs.converter.scalars)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit — распознавание штрихкодов/QR
    implementation(libs.barcode.scanning)

    // Lifecycle (для HandleScanResult)
    implementation(libs.androidx.lifecycle.runtime.compose)
}