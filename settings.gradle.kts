pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.8.0"
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("org.jetbrains.kotlin.kapt") version "1.9.22"
        id("com.google.dagger.hilt.android") version "2.48"
        id("com.google.gms.google-services") version "4.4.4" apply false
        id("io.gitlab.arturbosch.detekt") version "1.23.4"
        id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DriveTheoryCbt"
include(":app")
