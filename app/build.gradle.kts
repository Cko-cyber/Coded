plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.0"
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android") version "2.51.1"
    id("kotlin-kapt")
}

android {
    namespace = "com.example.coded"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.coded"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Supabase credentials
        buildConfigField("String", "SUPABASE_URL", "\"https://vxetgoaowehxxifdbdmm.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4ZXRnb2Fvd2VoeHhpZmRiZG1tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MjE4MzYsImV4cCI6MjA3NjQ5NzgzNn0.n7V7iNzNkMjzb2aNq_Z2ANoC7EhmdQcFB4H3tRBnNVM\"")
    }

    buildTypes {
        release {
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Compose
    implementation("androidx.compose.ui:ui:1.7.3")
    implementation("androidx.compose.ui:ui-graphics:1.7.3")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.3")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.3")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")

    // UI
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.accompanist:accompanist-permissions:0.35.0-alpha")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Accompanist
    implementation("com.google.accompanist:accompanist-pager:0.35.0-alpha")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.35.0-alpha")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:2.5.2"))
    implementation("io.github.jan-tennert.supabase:gotrue-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Ktor
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.3")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Profile Installer
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.3")

    // Added for app functionality
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.code.gson:gson:2.11.0")
}