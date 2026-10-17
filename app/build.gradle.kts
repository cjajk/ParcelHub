plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.cjajk.parcelhub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cjajk.parcelhub"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        val props = java.util.Properties().apply {
            runCatching { load(rootProject.file("local.properties").inputStream()) }
        }
        val customer = props.getProperty("KD100_CUSTOMER") ?: System.getenv("KD100_CUSTOMER") ?: ""
        val key = props.getProperty("KD100_KEY") ?: System.getenv("KD100_KEY") ?: ""
        buildConfigField("String", "KD100_CUSTOMER", "\"$customer\"")
        buildConfigField("String", "KD100_KEY", "\"$key\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
