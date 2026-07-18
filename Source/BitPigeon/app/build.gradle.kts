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
        versionCode = 5
        versionName = "1.0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                // Forces the linker to use 16KB page alignment
                arguments("-DANDROID_ALIGNED_16K=ON")
                cppFlags("-Wl,-z,max-page-size=16384")
            }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.firebase.firestore)
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
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.0")

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
    implementation(libs.coil.video)

    // Image processing and graphics and media
    implementation("androidx.graphics:graphics-core:1.0.4")
    implementation("androidx.compose.foundation:foundation:1.8.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Media3 ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)

    // Telephoto for zoomable images
    implementation(libs.telephoto.zoomable.image.coil3)

    // Lifecycle runtime compose for LocalLifecycleOwner
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
}
