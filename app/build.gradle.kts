plugins {
    alias(libs.plugins.google.services)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.finance_code"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.finance_code"
        minSdk = 28
        targetSdk = 36
        versionCode = 7
        versionName = "2.00"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        packaging {
            resources {
                excludes.add("META-INF/DEPENDENCIES")
                excludes.add("META-INF/LICENSE")
                excludes.add("META-INF/LICENSE.txt")
                excludes.add("META-INF/NOTICE")
                excludes.add("META-INF/NOTICE.txt")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
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
}

dependencies {

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- FIREBASE (Centralizado y limpio con un solo BoM) ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-crashlytics")

    // --- SERVICIOS DE GOOGLE Y AUTENTICACIÓN ---
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.client.android)
    implementation("com.google.api-client:google-api-client-android:2.2.0")

    // --- LIBRERÍAS DE INTERFAZ Y UTILIDADES ---
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.biometric)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // --- GOOGLE PLAY CONSOLE ---
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation("com.google.android.play:review-ktx:2.0.1")

    // Google AI Edge SDK para inferencia local
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    // ML Kit para extracción de entidades (montos, fechas) sin API
    implementation("com.google.mlkit:entity-extraction:16.0.0-beta5")
    // Corrutinas para no bloquear la UI
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}