import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // Applied only when google-services.json exists (created from the Firebase console).
    // Until then the app builds without Firebase and uses the Pocket ID OIDC sign-in.
    // The plugins DSL has no projectDir/rootDir, so probe the plausible relative
    // locations for the file depending on where Gradle is invoked from.
    if (File("google-services.json").exists()
        || File("app/google-services.json").exists()
        || File("android/app/google-services.json").exists()
    ) {
        id("com.google.gms.google-services")
    }
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun cfg(key: String): String =
    (localProps.getProperty(key) ?: providers.gradleProperty(key).orNull ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "app.ours"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.ours"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_APP_ID", "\"${cfg("ours.firebase.appId")}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${cfg("ours.firebase.apiKey")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${cfg("ours.firebase.projectId")}\"")
        buildConfigField("String", "FIREBASE_SENDER_ID", "\"${cfg("ours.firebase.senderId")}\"")
        val bakedServerUrl = cfg("OURS_SERVER_URL").ifBlank { cfg("ours.defaultServerUrl") }
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"$bakedServerUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${cfg("ours.googleWebClientId")}\"")
        // Pocket ID OIDC ANDROID client (native sign-in). Falls back to /api/config oidc block.
        buildConfigField("String", "OURS_ANDROID_CLIENT_ID", "\"${cfg("ours.androidClientId")}\"")
    }

    signingConfigs {
        val keystorePath = System.getenv("OURS_KEYSTORE_PATH") ?: cfg("ours.keystore.path")
        if (keystorePath.isNotBlank() && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("OURS_KEYSTORE_PASSWORD") ?: cfg("ours.keystore.password")
                keyAlias = System.getenv("OURS_KEY_ALIAS") ?: cfg("ours.key.alias")
                keyPassword = System.getenv("OURS_KEY_PASSWORD") ?: cfg("ours.key.password")
            }
        }
    }

    buildTypes {
        debug {
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.maplibre)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.firebase.messaging)
    // Firebase Auth — used only when google-services.json is present (the plugin
    // above is conditionally applied). On a build without it this lib is still
    // packaged but unused; the sign-in flow picks Pocket ID OIDC instead.
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.identity.googleid)
    // Legacy Google Sign-In (GoogleSignInClient / signInIntent) as a fallback
    // when Credential Manager's identitycredentials process crashes
    // (DeadObjectException on BadAuthentication). This path renders the native
    // account picker via a different GMS code path that does NOT go through the
    // identitycredentials process. It is NOT deprecated and still works on
    // Android 16. Already transitively available from credentials-play-services-auth,
    // but declared explicitly so the classpath is robust.
    implementation("com.google.android.gms:play-services-auth:21.1.1")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

val syncWebAssets by tasks.registering(Copy::class) {
    val webDir = rootProject.file("../web")
    onlyIf { webDir.exists() }
    from(webDir)
    into(layout.projectDirectory.dir("src/main/assets/web"))
}

tasks.named("preBuild") {
    dependsOn(syncWebAssets)
}
