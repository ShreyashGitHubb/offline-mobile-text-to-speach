plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.offlinetts"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.offlinetts"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // NDK config might be needed for some native libs, but libs usually handle it.
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

    // Espeak-ng (User recommended)
    // Note: Assuming this is available on JitPack or similar. 
    // If not found, we might need a local aar or a different fork.
    implementation("com.github.crushing-tides:libespeak-ng-android:master-SNAPSHOT")
    // Fallback/Alternative if the above fails: use a known working fork or build
    
    // FFmpeg
    // Using mobile-ffmpeg-audio as requested. 
    // Note: This library is old (replaced by ffmpeg-kit), but matches the user's specific "Holy Grail" list.
    implementation("com.arthenica:mobile-ffmpeg-audio:4.4") 
    // Alternatively: implementation("com.arthenica:ffmpeg-kit-audio:6.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
