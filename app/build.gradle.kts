import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.5"
}

fun loadOptionalProperties(path: String): Properties = Properties().apply {
    val file = rootProject.file(path)
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun firstNonBlank(vararg values: String?): String {
    return values.firstOrNull { !it.isNullOrBlank() }.orEmpty()
}

val trackedGradleProperties = loadOptionalProperties("gradle.properties")
check(trackedGradleProperties.getProperty("GEMINI_API_KEY").isNullOrBlank()) {
    "Non salvare GEMINI_API_KEY nel gradle.properties del progetto. " +
        "Usa secrets.properties (ignorato da git), local.properties, " +
        "GEMINI_API_KEY nell'ambiente o il gradle.properties utente."
}

val secretsProperties = loadOptionalProperties("secrets.properties")
val localProperties = loadOptionalProperties("local.properties")

val geminiApiKey = firstNonBlank(
    providers.environmentVariable("GEMINI_API_KEY").orNull,
    secretsProperties.getProperty("GEMINI_API_KEY"),
    localProperties.getProperty("GEMINI_API_KEY"),
    providers.gradleProperty("GEMINI_API_KEY").orNull
)
    .replace("\"", "\\\"")

android {
    namespace = "it.lagioiaproductions.nutrislot"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "it.lagioiaproductions.nutrislot"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 6
        versionName = "6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        }
        release {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom.v20260300))
    implementation(libs.androidx.compose.foundation)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20260300))

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.pdfbox.android)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.runtime.saveable)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    androidTestImplementation(libs.androidx.ui.test.junit4)
}
