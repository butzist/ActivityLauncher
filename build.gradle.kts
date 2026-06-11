buildscript {
    repositories {
        maven { url = uri("https://artifacts.applovin.com/android") }
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.9.8")
        classpath("com.google.gms:google-services:4.4.4")
        classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.7")
        classpath("com.google.firebase:perf-plugin:2.0.2")
        classpath("com.applovin.quality:AppLovinQualityServiceGradlePlugin:5.12.5")
    }
}

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
    id("com.diffplug.spotless") version "8.6.0" apply false
}
