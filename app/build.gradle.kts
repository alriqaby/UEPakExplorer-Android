plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.uepakexplorer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.uepakexplorer"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    buildFeatures { buildConfig = true }

buildTypes {
    getByName("release") {
        signingConfig = signingConfigs.getByName("debug")
    }
}
    packaging { jniLibs { useLegacyPackaging = false } }
}

kotlin { jvmToolchain(17) }

tasks.register<Exec>("buildRustArm64") {
    workingDir(project.file("src/main/rust"))
    commandLine(
        "cargo", "ndk", "-t", "arm64-v8a",
        "-o", project.file("$buildDir/generated/jniLibs").absolutePath,
        "build", "--release"
    )
}

tasks.named("preBuild") { dependsOn("buildRustArm64") }
android.sourceSets["main"].jniLibs.srcDir("$buildDir/generated/jniLibs")
