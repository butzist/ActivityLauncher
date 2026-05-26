pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven("https://artifacts.applovin.com/android")
        maven("https://android-sdk.is.com/")
        maven("https://artifact.bytedance.com/repository/pangle/")
        maven("https://cboost.jfrog.io/artifactory/chartboost-ads/")
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
        maven("https://repo.pubmatic.com/artifactory/public-repos/")
        maven("https://maven.ogury.co")
        maven("https://s3.amazonaws.com/smaato-sdk-releases/")
        maven("https://verve.jfrog.io/artifactory/verve-gradle-release")
    }
}

rootProject.name = "ActivityLauncher"
include(":app")
