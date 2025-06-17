plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "de.szalkowski.activitylauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = System.getenv("APPID") ?: "de.szalkowski.activitylauncher"
        minSdk = 16
        targetSdk = 36
        versionCode = 64
        versionName = "2.1.4"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("oss") {
            // for direct distribution
            dimension = "distribution"
        }
        create("playStore") {
            // includes Google Play Store specific additions
            dimension = "distribution"
            minSdk = 23
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE") ?: "keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.dagger:hilt-android:2.50")
    "playStoreImplementation"("com.google.android.play:review-ktx:2.0.2")
    kapt("com.google.dagger:hilt-compiler:2.50")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
