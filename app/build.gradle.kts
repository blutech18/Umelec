plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.umelec"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.umelec"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

// Workaround for locked R.jar file on Windows
val cleanLockedResources = tasks.register("cleanLockedResources") {
    val buildDir = layout.buildDirectory
    doLast {
        // Try to delete the entire compile_and_runtime_not_namespaced_r_class_jar directory
        // This is more aggressive and should prevent the Android plugin from trying to delete individual files
        val parentDir = buildDir.get().asFile.resolve("intermediates/compile_and_runtime_not_namespaced_r_class_jar")
        if (parentDir.exists()) {
            try {
                // First, try to delete the entire directory structure
                val deleted = parentDir.deleteRecursively()
                if (!deleted && parentDir.exists()) {
                    // If that fails, try to delete just the debug subdirectory
                    val debugDir = parentDir.resolve("debug")
                    if (debugDir.exists()) {
                        try {
                            debugDir.deleteRecursively()
                            logger.warn("Deleted debug subdirectory, but parent directory may still exist")
                        } catch (e: Exception) {
                            logger.warn("Could not delete debug directory: ${e.message}")
                        }
                    }
                    // Try to delete the processDebugResources directory specifically
                    val processDir = parentDir.resolve("debug/processDebugResources")
                    if (processDir.exists()) {
                        try {
                            // Try to delete all files in the directory first
                            processDir.listFiles()?.forEach { file ->
                                try {
                                    file.setWritable(true, false)
                                    if (!file.delete()) {
                                        file.renameTo(file.resolveSibling("${file.name}.old"))
                                    }
                                } catch (e: Exception) {
                                    // Ignore individual file errors
                                }
                            }
                            processDir.deleteRecursively()
                        } catch (e: Exception) {
                            logger.warn("Could not delete processDebugResources directory: ${e.message}")
                            logger.warn("Please close Android Studio and try again")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error cleaning locked resources: ${e.message}")
                logger.warn("You may need to close Android Studio to release file locks")
            }
        }
    }
}

// Configure processDebugResources after Android plugin creates it
// Using tasks.matching to avoid configuration cache issues
tasks.matching { it.name == "processDebugResources" }.configureEach {
    dependsOn(cleanLockedResources)
    doFirst {
        // One final attempt to clean up before the Android plugin tries to delete
        val buildDir = layout.buildDirectory.get().asFile
        val rJarFile = buildDir.resolve("intermediates/compile_and_runtime_not_namespaced_r_class_jar/debug/processDebugResources/R.jar")
        if (rJarFile.exists()) {
            try {
                rJarFile.setWritable(true, false)
                if (!rJarFile.delete()) {
                    // Try renaming one more time
                    val renamed = rJarFile.resolveSibling("R.jar.locked")
                    if (rJarFile.renameTo(renamed)) {
                        logger.warn("Renamed R.jar to R.jar.locked as last resort")
                    } else {
                        logger.error("CRITICAL: R.jar is locked and cannot be deleted or renamed!")
                        logger.error("Please close Android Studio completely and try again.")
                        throw GradleException("Cannot delete locked R.jar file. Please close Android Studio and retry the build.")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to clean R.jar: ${e.message}")
                logger.error("Please close Android Studio completely and try again.")
                throw GradleException("Cannot delete locked R.jar file. Please close Android Studio and retry the build.", e)
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.google.android.material:material:1.13.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("com.github.gcacace:signature-pad:1.3.1")
    
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
}