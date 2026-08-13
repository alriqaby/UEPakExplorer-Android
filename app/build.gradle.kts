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

        versionCode = 3
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
                ?: error("KEYSTORE_FILE is not set")

            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: error("KEYSTORE_PASSWORD is not set")

            val keyAlias = System.getenv("KEY_ALIAS")
                ?: error("KEY_ALIAS is not set")

            storeFile = file(keystoreFile)
            storePassword = keystorePassword
            this.keyAlias = keyAlias
            keyPassword = keystorePassword
            storeType = "PKCS12"
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.register<Exec>("buildRustArm64") {
    workingDir(project.file("src/main/rust"))

    commandLine(
        "cargo",
        "ndk",
        "-t",
        "arm64-v8a",
        "-o",
        project.file("$buildDir/generated/jniLibs").absolutePath,
        "build",
        "--release"
    )
}

tasks.named("preBuild") {
    dependsOn("buildRustArm64")
}

android.sourceSets["main"].jniLibs.srcDir(
    "$buildDir/generated/jniLibs"
)
