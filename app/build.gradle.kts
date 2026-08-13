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
            val keystorePassword = providers.environmentVariable("KEYSTORE_PASSWORD").orNull
            val keyAlias = providers.environmentVariable("KEY_ALIAS").orNull

            if (keystoreFile != null && keystorePassword != null && keyAlias != null) {
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
    }

    buildTypes {
        getByName("release") {
            val hasReleaseSigning =
                providers.environmentVariable("KEYSTORE_FILE").orNull != null &&
                providers.environmentVariable("KEYSTORE_PASSWORD").orNull != null &&
                providers.environmentVariable("KEY_ALIAS").orNull != null

            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }

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

tasks.register("verifyReleaseSigning") {
    doLast {
        val required = listOf(
            "KEYSTORE_FILE",
            "KEYSTORE_PASSWORD",
            "KEY_ALIAS"
        )

        val missing = required.filter {
            System.getenv(it).isNullOrBlank()
        }

        if (missing.isNotEmpty()) {
            error(
                "Official release signing is not configured. Missing: ${missing.joinToString()}"
            )
        }
    }
}

tasks.matching {
    it.name == "preReleaseBuild"
}.configureEach {
    dependsOn("verifyReleaseSigning")
}
