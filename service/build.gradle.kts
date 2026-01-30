plugins {
    id(Plugins.androidLibrary)
    id(Plugins.kotlinAndroid)
}

android {
    namespace = "com.navajasuiza.service"
    compileSdk = AppConfig.compileSdk

    defaultConfig {
        minSdk = AppConfig.minSdk
    }
    
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ocr"))
    implementation(project(":data")) // Service might need to save to DB
    implementation(project(":features:history"))
    implementation(Libs.coreKtx)
    implementation(Libs.lifecycleRuntime)
    implementation(Libs.appCompat) // For WindowManager, etc.
    implementation(Libs.constraintLayout) // For overlays
    implementation(Libs.dynamicAnimation)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(Libs.material)
}
