plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
    // Add serialization plugin for Supabase
    kotlin("plugin.serialization") version "1.9.22"
}

        android {
    namespace = "com.example.coded"
    compileSdk = 36  // Changed from 36 to stable version

    defaultConfig {
        applicationId = "com.example.coded"
        minSdk = 26
        targetSdk = 35  // Changed from 36 to stable version
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase credentials
        buildConfigField("String", "SUPABASE_URL", "\"https://vxetgoaowehxxifdbdmm.supabase.co\"")
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ4ZXRnb2Fvd2VoeHhpZmRiZG1tIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA5MjE4MzYsImV4cCI6MjA3NjQ5NzgzNn0.n7V7iNzNkMjzb2aNq_Z2ANoC7EhmdQcFB4H3tRBnNVM\""
        )
    }

    configurations.all {
        resolutionStrategy {
            // Force specific versions to avoid conflicts
            force("androidx.browser:browser:1.7.0")
            force("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
        }
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"  // Compatible with Kotlin 1.9.22
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

        dependencies {// Use the latest Supabase BOM
            implementation(platform("io.github.jan-tennert.supabase:bom:3.2.4"))
            implementation("io.github.jan-tennert.supabase:auth-kt")
            implementation("io.github.jan-tennert.supabase:postgrest-kt")
            implementation("io.github.jan-tennert.supabase:storage-kt")
            implementation("io.github.jan-tennert.supabase:realtime-kt")
// Ktor Client for Supabase (use compatible 3.x version)
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            implementation("io.ktor:ktor-client-core:3.3.0")
            implementation("io.ktor:ktor-client-android:3.3.0")
            implementation("io.ktor:ktor-client-cio:3.3.0")
            implementation("io.ktor:ktor-client-content-negotiation:3.3.0")
            implementation("io.ktor:ktor-client-logging:3.3.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.0")
// Kotlinx Serialization (for JSON handling)
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
// Core AndroidX
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.activity.compose)
// Compose BOM and UI
            implementation(platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3)
            implementation("androidx.compose.material:material-icons-extended:1.6.0")
            implementation("androidx.compose.runtime:runtime-livedata:1.6.0")
// Lifecycle & ViewModel
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
// Navigation
            implementation("androidx.navigation:navigation-compose:2.7.7")
// Browser (for OAuth flows)
            implementation("androidx.browser:browser:1.7.0")
// Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
// Firebase (ONLY for messaging/notifications)
            implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
            implementation("com.google.firebase:firebase-messaging-ktx")
// Google Maps
            implementation("com.google.maps.android:maps-compose:4.3.3")
            implementation("com.google.android.gms:play-services-maps:18.2.0")
            implementation("com.google.android.gms:play-services-location:21.1.0")
            implementation("com.google.maps.android:android-maps-utils:3.8.2")
// Permissions
            implementation("com.google.accompanist:accompanist-permissions:0.34.0")
// Image Loading
            implementation("io.coil-kt:coil-compose:2.5.0")
// JSON & Network
            implementation("com.google.code.gson:gson:2.10.1")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
// Dependency Injection (Hilt)
            implementation("com.google.dagger:hilt-android:2.51.1")
            kapt("com.google.dagger:hilt-compiler:2.51.1")
            implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
// Testing
            testImplementation(libs.junit)
            testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            testImplementation("io.mockk:mockk:1.13.9")
            androidTestImplementation(libs.androidx.junit)
            androidTestImplementation(libs.androidx.espresso.core)
            androidTestImplementation(platform(libs.androidx.compose.bom))
            androidTestImplementation(libs.androidx.compose.ui.test.junit4)
            debugImplementation(libs.androidx.compose.ui.tooling)
            debugImplementation(libs.androidx.compose.ui.test.manifest)
        }
// Ensure kapt configuration is correct
kapt {
    correctErrorTypes = true
    useBuildCache = true
}