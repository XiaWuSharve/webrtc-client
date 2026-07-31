plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.1.21"
    id("com.google.protobuf") version "0.10.0"
}

android {
    namespace = "com.github.xiawusharve.webrtc"
    compileSdk {
        version = release(37) {
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
    implementation(libs.protobuf.javalite)
    // Source: https://mvnrepository.com/artifact/com.google.protobuf/protobuf-kotlin-lite
    implementation(libs.protobuf.kotlin.lite)
    // Source: https://mvnrepository.com/artifact/androidx.lifecycle/lifecycle-viewmodel-compose
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.datastore.preferences)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.35.1"
    }
    generateProtoTasks {
        all().configureEach {
            // 这里的 this 就是 GenerateProtoTask 对象
            builtins {
                // 用 create 代替直接调用 java { }
                create("java") {
                    option("lite")
                }
                // Kotlin Lite 生成
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}