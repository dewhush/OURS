plugins {
    alias(libs.plugins.android.application) apply false
    // Declared so AGP's built-in Kotlin uses this KGP version (matches the compose compiler).
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
