// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id(Plugins.androidApplication) version Versions.agp apply false
    id(Plugins.androidLibrary) version Versions.agp apply false
    id(Plugins.kotlinAndroid) version Versions.kotlin apply false
}

task("clean", Delete::class) {
    delete(rootProject.buildDir)
}