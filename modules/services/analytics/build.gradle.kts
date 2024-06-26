plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
}

apply(from = "${project.rootDir}/base.gradle")

android {
    namespace = "au.com.shiftyjelly.pocketcasts.analytics"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.tracks)
    implementation(project(":modules:services:utils"))
    implementation(project(":modules:services:preferences"))
    implementation(project(":modules:services:model"))
}
