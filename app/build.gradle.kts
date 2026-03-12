plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("androidx.navigation.safeargs.kotlin")
    id("com.diffplug.spotless")
}

android {
    namespace = "de.szalkowski.activitylauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = System.getenv("APPID") ?: "de.szalkowski.activitylauncher"
        minSdk = 16
        targetSdk = 36
        versionCode = 71
        versionName = "2.2.1"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += listOf("distribution", "ads")
    productFlavors {
        create("oss") {
            dimension = "distribution"
            minSdk = 16
        }
        create("playStore") {
            dimension = "distribution"
            minSdk = 23
        }
    }

    productFlavors {
        create("noads") {
            dimension = "ads"
            resValue("string", "admob_banner_id", "unused")
        }
        create("ads") {
            dimension = "ads"
            val bannerId = System.getenv("ADMOB_BANNER_ID") ?: "ca-app-pub-3940256099942544/6300978111"
            val appId = System.getenv("ADMOB_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
            resValue("string", "admob_banner_id", bannerId)
            manifestPlaceholders["ADMOB_APP_ID"] = appId
        }
    }

    variantFilter {
        val distribution = flavors.find { it.dimension == "distribution" }?.name
        val ads = flavors.find { it.dimension == "ads" }?.name
        if (distribution == "oss" && ads == "ads") {
            ignore = true
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
                "proguard-rules.pro",
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
        buildConfig = true
    }
}

// Conditionally apply Google services and Firebase plugins
// These are only applied if the 'ads' flavor is present in the current build task
val taskNames = gradle.startParameter.taskNames.joinToString(",")
val isAdsBuild = !taskNames.contains("Noads", ignoreCase = true)

if (isAdsBuild) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
    apply(plugin = "com.google.firebase.firebase-perf")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

// Configure Spotless for code formatting
spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint("1.2.1").editorConfigOverride(
            mapOf(
                "android" to "true",
                "ktlint_standard_no-wildcard-imports" to "disabled",
                "ktlint_standard_chain-rule-first" to "disabled",
                "ktlint_standard_backing-property-naming" to "disabled",
            ),
        )
    }

    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**/*.kts")
        ktlint("1.2.1")
    }

    format("xml") {
        target("**/*.xml")
        targetExclude("**/build/**/*.xml")
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Configure build-time formatting for development
tasks.named("preBuild") {
    // Auto-format on development builds, but check on CI
    if (System.getenv("CI") == null) {
        dependsOn("spotlessApply")
    } else {
        dependsOn("spotlessCheck")
    }
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

    "adsImplementation"("com.google.android.gms:play-services-ads:22.6.0")
    "adsImplementation"(platform("com.google.firebase:firebase-bom:30.1.0"))
    "adsImplementation"("com.google.firebase:firebase-analytics")
    "adsImplementation"("com.google.firebase:firebase-crashlytics")
    "adsImplementation"("com.google.firebase:firebase-analytics-ktx")
    "adsImplementation"("com.google.firebase:firebase-perf")
    "adsImplementation"("com.google.android.ump:user-messaging-platform:2.2.0")
    "adsImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    kapt("com.google.dagger:hilt-compiler:2.50")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
