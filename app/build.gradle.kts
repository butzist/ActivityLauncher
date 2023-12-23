apply plugin: 'com.android.application'
android {
    signingConfigs {
        release {
            storeFile file(System.getenv("KEYSTORE") ?: "keystore.jks")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    compileSdk 33
    defaultConfig {
        applicationId "de.szalkowski.activitylauncher"
        minSdkVersion 19
        targetSdkVersion 33
    }
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            signingConfig signingConfigs.release
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.txt'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        viewBinding true
    }
    namespace 'de.szalkowski.activitylauncher'
}

dependencies {
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
    implementation 'androidx.fragment:fragment:1.6.1'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.preference:preference:1.2.1'
}

configurations.implementation {
    exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
}
