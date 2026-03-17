// build.gradle.kts (app level)

plugins {
    alias(libs.plugins.kotlin.compose)
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.budgetapp.budgetapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.budgetapp.budgetapp"
        minSdk = 25
        targetSdk = 35
        versionCode = 14
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // <--- ABILITA QUESTA OPZIONE
            isShrinkResources = true // <--- OPZIONALE MA CONSIGLIATO PER RIDURRE LE DIMENSIONI
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ndk { debugSymbolLevel } rimosso: causa warning con .so già strippate di Compose
        }
    }
    packaging {
        jniLibs {
            // Esclude la .so di Compose già strippata che non può essere ri-processata
            excludes += setOf("**/libandroidx.graphics.path.so")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Abilita desugaring per API basse
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // You generally don't need to specify kotlinCompilerExtensionVersion here
        // if you're using the compose plugin from the libs.versions.toml and
        // your Kotlin version is correctly linked.
        // It's usually inferred from the plugin or bom.
    }
}

dependencies {
    // Desugaring per java.time su API < 26
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Always use platform(libs.androidx.compose.bom) first to manage Compose versions
    implementation(platform(libs.androidx.compose.bom))

    // These compose dependencies will now get their versions from the composeBom
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // This is the main Material 3 library
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.ui.auth)

    // Kotlin Coroutines for Firebase
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.core)

    // Vico charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.core)

    // For tools and appcompat, keep direct implementations
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation(libs.androidx.ui.tooling)
}
