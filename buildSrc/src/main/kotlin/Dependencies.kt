object Versions {
    const val kotlin = "1.9.22"
    const val agp = "8.2.2" // Updated to a stable recent version
    const val coreKtx = "1.12.0"
    const val appCompat = "1.6.1"
    const val material = "1.11.0"
    const val constraintLayout = "2.1.4"
    const val lifecycle = "2.7.0"
    const val room = "2.6.1"
    const val coroutines = "1.7.3"
    const val mlKitText = "19.0.0"
    const val startup = "1.1.1"
    const val activityKtx = "1.8.2"
    const val fragmentKtx = "1.6.2"
    const val dynamicAnimation = "1.0.0"
}

object AppConfig {
    const val applicationId = "com.jhosue.gravilens"
    const val compileSdk = 34
    const val minSdk = 26
    const val targetSdk = 34
    const val versionCode = 1
    const val versionName = "1.0.0"
}

object Libs {
    // AndroidX
    const val coreKtx = "androidx.core:core-ktx:${Versions.coreKtx}"
    const val appCompat = "androidx.appcompat:appcompat:${Versions.appCompat}"
    const val material = "com.google.android.material:material:${Versions.material}"
    const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
    const val startup = "androidx.startup:startup-runtime:${Versions.startup}"
    
    // Lifecycle
    const val lifecycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
    const val lifecycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    const val activityKtx = "androidx.activity:activity-ktx:${Versions.activityKtx}"
    
    // Coroutines
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    const val coroutinesPlayServices = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Versions.coroutines}"
    
    // Room
    const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    const val roomKtx = "androidx.room:room-ktx:${Versions.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
    
    // ML Kit
    const val mlKitText = "com.google.android.gms:play-services-mlkit-text-recognition:${Versions.mlKitText}"

    // Testing
    const val junit = "junit:junit:4.13.2"
    const val extJunit = "androidx.test.ext:junit:1.1.5"
    const val espresso = "androidx.test.espresso:espresso-core:3.5.1"

    // Animation
    const val dynamicAnimation = "androidx.dynamicanimation:dynamicanimation:${Versions.dynamicAnimation}"
}

object Plugins {
    const val androidApplication = "com.android.application"
    const val androidLibrary = "com.android.library"
    const val kotlinAndroid = "org.jetbrains.kotlin.android"
    const val kapt = "org.jetbrains.kotlin.kapt"
}
