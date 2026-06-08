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
        applicationId =
            providers.environmentVariable("APPID").getOrElse("de.szalkowski.activitylauncher")
        minSdk = 16
        targetSdk = 36
        versionCode = 7502
        versionName = "2.2.5"

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
        create("noads") {
            dimension = "ads"
            resValue("string", "admob_banner_id", "unused")
            resValue("string", "publisher_id", "")
            resValue("string", "app_id", "")
        }
        create("ads") {
            dimension = "ads"
            val admobAppId =
                providers.environmentVariable("ADMOB_APP_ID").get()
            val publisherId = providers.environmentVariable("PUBLISHER_ID").get()
            val appId = providers.environmentVariable("APP_ID").get()
            manifestPlaceholders["ADMOB_APP_ID"] = admobAppId
            resValue("string", "publisher_id", publisherId)
            resValue("string", "app_id", appId)
        }
    }
    signingConfigs {
        create("release") {
            val keystorePath = providers.environmentVariable("KEYSTORE").orElse("keystore.jks")
            storeFile = file(keystorePath)
            storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
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

androidComponents {
    beforeVariants(selector().withFlavor("distribution", "oss").withFlavor("ads", "ads")) {
        it.enable = false
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
    apply(plugin = "applovin-quality-service")
}

// Configure AppLovin Quality Service if applied
if (isAdsBuild) {
    extensions.findByName("applovin")?.let { extension ->
        val adReviewKey = providers.environmentVariable("AD_REVIEW_KEY").get()
        val method = extension.javaClass.methods.find { it.name == "setApiKey" }
        method?.invoke(extension, adReviewKey)
    }
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
    javacOptions {
        option("-Xmaxerrs", "500")
    }
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
    implementation("com.google.dagger:hilt-android:2.55")
    "playStoreImplementation"("com.google.android.play:review-ktx:2.0.2")

    "adsImplementation"(platform("com.google.firebase:firebase-bom:33.10.0"))
    "adsImplementation"("com.google.firebase:firebase-analytics")
    "adsImplementation"("com.google.firebase:firebase-crashlytics")
    "adsImplementation"("com.google.firebase:firebase-perf")
    "adsImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    "adsImplementation"("com.intergi.playwire:playwiresdk_total:12.1.1")
    "adsImplementation"("com.applovin:applovin-sdk:13.6.2")
    "adsImplementation"("com.google.android.gms:play-services-ads-identifier:18.1.0")

    kapt("com.google.dagger:hilt-compiler:2.55")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
