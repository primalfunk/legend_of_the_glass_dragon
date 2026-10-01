plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

tasks.withType<Test>().configureEach {
    providers.gradleProperty("lgd011aSeasons").orNull?.let {
        systemProperty("lgd011a.seasons", it)
    }
    providers.gradleProperty("lgd011aOutput").orNull?.let {
        systemProperty("lgd011a.output", it)
    }
    providers.gradleProperty("lgd011bOutput").orNull?.let {
        systemProperty("lgd011b.output", it)
    }
    providers.gradleProperty("lgd011bRound").orNull?.let {
        systemProperty("lgd011b.round", it)
    }
    providers.gradleProperty("lgd011bSeasons").orNull?.let {
        systemProperty("lgd011b.seasons", it)
    }
}

android {
    namespace = "com.legendoftheglassdragon"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.legendoftheglassdragon"
        minSdk = 26
        targetSdk = 37
        versionCode = 12
        versionName = "1.0.0-rc1"

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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
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
}
