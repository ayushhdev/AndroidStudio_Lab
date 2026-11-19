plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.qrvault"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.qrvault"
        // API 29+ lets MediaStore write to the gallery without a storage permission.
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    // ZXing: core encodes the QR matrix; android-embedded supplies the camera scanner.
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
