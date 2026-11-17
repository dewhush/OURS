plugins {
    alias(libs.plugins.android.application) apply false
    // Declared so AGP's built-in Kotlin uses this KGP version (matches the compose compiler).
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // Firebase Auth + Google Sign-In plugin. Applied in the app module ONLY when
    // app/google-services.json exists, so the build still works before the
    // Firebase project is created (falls back to the Pocket ID OIDC path).
    alias(libs.plugins.google.services) apply false
}
