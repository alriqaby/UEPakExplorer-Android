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
            val keystoreFile = providers.environmentVariable("KEYSTORE_FILE").orNull
                ?: error("KEYSTORE_FILE is not set. Official releases must provide the private keystore.")

            val keystorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
                ?: error("KEYSTORE_PASSWORD is not set. Official releases must provide the keystore password.")

            val keyAlias = providers.environmentVariable("KEY_ALIAS").orNull
                ?: error("KEY_ALIAS is not set. Official releases must provide the key alias.")

            val releaseKeystore = file(keystoreFile)

            if (!releaseKeystore.isFile) {
                error("Release keystore does not exist: $keystoreFile")
            }

            storeFile = releaseKeystore
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
