# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# For more details, see
#   https://developer.android.com/studio/build/shrink-code.html

# Keep the application class
-keep class com.bodla.parivar.MainActivity { *; }

# Keep Firebase Messaging Service
-keep class com.bodla.parivar.BodlaFirebaseMessagingService { *; }

# Ktor HTTP Client
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# SQLDelight
-keep class com.bodla.parivar.database.** { *; }
-dontwarn com.bodla.parivar.database.**

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }

# Compose Multiplatform
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Material3
-keep class androidx.compose.material3.** { *; }

# Coil image loading
-keep class coil3.compose.** { *; }
-dontwarn coil3.compose.**

# Supabase / Ktor client
-keep class com.bodla.parivar.core.network.** { *; }

# Keep native method names
-keepclasseswithmembers class * {
    native <methods>;
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# R8 missing classes - test dependencies and SLF4J
-dontwarn androidx.test.platform.app.InstrumentationRegistry
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn org.slf4j.**
