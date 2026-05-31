plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
//    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.devtools.ksp)
}


android {
    namespace = "com.codingskillshub.bitpigeon"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.codingskillshub.bitpigeon"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val room_version = "2.6.1"

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // Hilt for Jetpack Compose Navigation
    implementation(libs.hilt.navigation.compose)

    // Room Runtime
    implementation("androidx.room:room-runtime:$room_version")
    // Room KSP (Annotation Processor)
    ksp("androidx.room:room-compiler:$room_version")
    // Optional: Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")
    // Optional: Test helpers
    testImplementation("androidx.room:room-testing:$room_version")

    // Coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // Image processing and graphics
    implementation("androidx.graphics:graphics-core:1.0.0-alpha03")
    implementation("androidx.compose.foundation:foundation:1.8.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
