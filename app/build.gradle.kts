import java.util.Calendar

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.salarycalc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.salarycalc"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("salarycalc") {
            storeFile = file("salarycalc.keystore")
            storePassword = "salary123"
            keyAlias = "salarycalc"
            keyPassword = "salary123"
            storeType = "PKCS12"
        }
    }

    buildTypes {
        // debug 不签名，构建 unsigned APK，本地统一用 jarsigner 签名
        debug {
            // signingConfig 暂时禁用，避免双重签名
        }
        release {
            signingConfig = signingConfigs["salarycalc"]
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

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
