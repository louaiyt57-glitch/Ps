plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pixelpet.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pixelpet.app"
        minSdk = 26          // TYPE_APPLICATION_OVERLAY يتطلب أندرويد 8.0+
        targetSdk = 35       // أندرويد 15
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // التوقيع بمفتاح debug كي يمكن تثبيت نسخة release مباشرة للتجربة
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// لا توجد اعتمادات خارجية عمداً: التطبيق يعتمد على إطار أندرويد فقط ليبقى خفيفاً وسريع البناء.
