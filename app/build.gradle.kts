plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
//    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
}

android {
    namespace = "com.github.xiawusharve.webrtc"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.github.xiawusharve.webrtc"
        minSdk = 24
        targetSdk = 36
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
//        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"                 // 已有的
            pickFirsts += "META-INF/io.netty.versions.properties"
            // 以下两行是预防性的，避免将来再报 native-image 相关文件的冲突
            pickFirsts += "META-INF/native-image/**"
            pickFirsts += "META-INF/services/**"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.java.websocket)
    implementation(libs.stream.webrtc.android)
    implementation(libs.permissionx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material.icons.core)
    // Source: https://mvnrepository.com/artifact/com.github.l42111996/kcp-base
    implementation(libs.kcp.base)
}