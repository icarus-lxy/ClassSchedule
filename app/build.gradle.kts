plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.classschedule.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.classschedule.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.9"
        ndk {
            // 识别引擎自带 4 种 CPU 架构的原生库，只保留手机实际在用的两种，APK 能小掉一大半
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    // 一套代码出多个独立 App：每个班/学校一个 flavor，各自的包名、名称、内置课表
    // （内置课表放在 app/src/<flavor>/java/.../data/SeedData.kt，资源在 app/src/<flavor>/res/）
    flavorDimensions.add("school")
    productFlavors {
        create("mine") {
            dimension = "school"
            applicationId = "com.classschedule.app"
            versionCode = 11
            versionName = "1.10"
        }
        create("linchuang") {
            dimension = "school"
            applicationId = "com.classschedule.linchuang"
            versionCode = 2
            versionName = "1.1"
        }
        create("tumu") {
            dimension = "school"
            applicationId = "com.classschedule.tumu"
            versionCode = 1
            versionName = "1.0"
        }
    }

    // 固定签名密钥随仓库提供：保证每次云端构建的 APK 签名一致，可以直接覆盖安装升级
    signingConfigs {
        create("fixed") {
            storeFile = file("../keystore/classschedule.keystore")
            storePassword = "classschedule"
            keyAlias = "classschedule"
            keyPassword = "classschedule"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fixed")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
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
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    // 离线中文文字识别（模型内置于 APK，不需要网络权限）
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
}
