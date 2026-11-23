plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.googleServices) apply false
    // Firebase plugin is applied only when google-services.json exists.
    // This avoids CI failures in forks/PRs where the secret is unavailable.
}

val uploadStoreFile = project.file(
    providers.gradleProperty("BODLA_UPLOAD_STORE_FILE")
        .orElse(providers.environmentVariable("BODLA_UPLOAD_STORE_FILE"))
        .orElse("upload-keystore.jks")
        .get()
)
val uploadStorePassword = providers.gradleProperty("BODLA_UPLOAD_STORE_PASSWORD")
    .orElse(providers.environmentVariable("BODLA_UPLOAD_STORE_PASSWORD")).orNull
val uploadKeyAlias = providers.gradleProperty("BODLA_UPLOAD_KEY_ALIAS")
    .orElse(providers.environmentVariable("BODLA_UPLOAD_KEY_ALIAS")).orNull
val uploadKeyPassword = providers.gradleProperty("BODLA_UPLOAD_KEY_PASSWORD")
    .orElse(providers.environmentVariable("BODLA_UPLOAD_KEY_PASSWORD")).orNull
val releaseSigningConfigured = uploadStoreFile.isFile &&
    !uploadStorePassword.isNullOrBlank() && !uploadKeyAlias.isNullOrBlank() && !uploadKeyPassword.isNullOrBlank()

if (file("${projectDir}/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        matching { it.name.contains("ios", ignoreCase = true) }.configureEach {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("androidx.compose.ui.ExperimentalComposeUiApi")
            languageSettings.optIn("androidx.compose.ui.text.ExperimentalTextApi")
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor2)

            // Coroutines & Serialization
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)

            // Dependency Injection
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Local SQLDelight Database
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)

            // Logging
            implementation(libs.napier)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.android.driver)
            implementation(compose.uiTooling)
            // Google Fonts (Downloadable Fonts for Anek Gujarati)
            implementation("androidx.compose.ui:ui-text-google-fonts:1.6.7")
            // Splash Screen API (Android 12+, backwards compat to API 23)
            implementation("androidx.core:core-splashscreen:1.0.1")
            // Firebase Cloud Messaging
            implementation(libs.firebase.messaging)
            implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
        }

        iosMain.dependencies {
            implementation(compose.ui)
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.bodla.parivar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bodla.parivar"
        minSdk = 26
        targetSdk = 36
        versionCode = 100137
        versionName = "10.01.37"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = uploadStoreFile
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.bodla.parivar.database")
        }
    }
}

compose.resources {
    packageOfResClass = "com.bodla.parivar.composeapp.generated.resources"
}

configurations.all {
    resolutionStrategy.force("com.google.android.recaptcha:recaptcha:18.4.0")
}
