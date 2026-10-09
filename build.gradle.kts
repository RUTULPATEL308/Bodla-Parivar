plugins {
    // Trick to apply plugins in subprojects without version specification
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.sqlDelight) apply false
}

allprojects {
    group = "com.bodla.parivar"
    version = "1.0.0"
}
