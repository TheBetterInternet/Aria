import java.util.Properties
import java.io.FileInputStream

val keystoreProperties = Properties().apply {
    val file = rootProject.file("key.properties")
    if (file.exists()) {
        load(FileInputStream(file))
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "org.thebetterinternet.aria"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.thebetterinternet.aria"
        minSdk = 21 // android 5 support, not sure it will work
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-nightly"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String?
            keyPassword = keystoreProperties["keyPassword"] as String?
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String?
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                signingConfig = signingConfigs.getByName("release")
                splits {
                    abi {
                        isEnable = true
                        reset()
                        include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                        isUniversalApk = false
                    }
                }
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
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
            compose = true
        }

        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.4"
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
    }

    dependencies {
        implementation(libs.androidx.core.ktx.v1160)
        implementation(libs.androidx.lifecycle.runtime.ktx.v270)
        implementation(libs.androidx.activity.compose.v182)
        //noinspection GradleDependency
        implementation(platform(libs.androidx.compose.bom.v20250501))
        implementation(libs.ui)
        implementation(libs.ui.graphics)
        implementation(libs.ui.tooling.preview)
        implementation(libs.geckoview)
        implementation(libs.androidx.material.icons.extended)
        implementation(libs.androidx.material3.android)
        implementation(libs.androidx.fragment.ktx)
        implementation(libs.androidx.viewpager2)
        implementation(libs.androidx.lifecycle.viewmodel.compose.android)
        implementation(libs.coil.compose)
        implementation(libs.androidx.core.splashscreen)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit.v121)
        androidTestImplementation(libs.androidx.espresso.core.v361)
        androidTestImplementation(platform(libs.compose.bom.v20231001))
        androidTestImplementation(libs.ui.test.junit4)
        debugImplementation(libs.ui.tooling)
        debugImplementation(libs.ui.test.manifest)
    }
}