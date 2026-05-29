plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Версия kotlin
    kotlin("jvm") version "2.2.10"
    // Соответствующая версия KSP
    id("com.google.devtools.ksp") version "2.3.6"
}