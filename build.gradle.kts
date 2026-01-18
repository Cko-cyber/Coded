plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false // Add this line
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "1.9.22" apply false
}