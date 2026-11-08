plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.memgallery"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.memgallery"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        // Số ảnh dùng cho demo (tối đa 40). Nếu bản unoptimized bị OOM/crash ngay
        // khi mở app thì giảm số này xuống (vd 20) và GHI LẠI trong báo cáo.
        buildConfigField("int", "PHOTO_COUNT", "40")
    }

    // ===== 2 bản trong cùng project: cài song song được trên 1 điện thoại =====
    flavorDimensions += "mode"
    productFlavors {
        create("unoptimized") {
            dimension = "mode"
            applicationIdSuffix = ".unopt"
            versionNameSuffix = "-unopt"
            resValue("string", "app_name", "Gallery UNOPT")
            buildConfigField("boolean", "OPTIMIZED", "false")
        }
        create("optimized") {
            dimension = "mode"
            applicationIdSuffix = ".opt"
            versionNameSuffix = "-opt"
            resValue("string", "app_name", "Gallery OPT")
            buildConfigField("boolean", "OPTIMIZED", "true")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Cho phép đo bản release bằng khóa debug (chỉ để demo/đo đạc)
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        resValues = true
    }
    // Không nén ảnh trong assets để đọc nhanh, nhất quán giữa 2 bản
    androidResources {
        noCompress += "jpg"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Phát hiện memory leak (chỉ bản debug, có ở cả 2 flavor)
    debugImplementation(libs.leakcanary)
}
