plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.jorso.carapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jorso.carapp"
        minSdk = 29
        targetSdk = 36
        versionCode = 24
        versionName = "1.1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("C:/Users/jorso/Documents/AACarEntertainment/aacarentertainment.jks")
            storePassword = "aacar2026"
            keyAlias = "aacarentertainment"
            keyPassword = "aacar2026"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.car.app.projected)
    implementation(libs.media3.exoplayer)
    implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.glide)
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("androidx.documentfile:documentfile:1.0.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}










