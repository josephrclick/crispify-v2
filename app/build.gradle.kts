plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Configure Java toolchain for consistent builds and test compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure test tasks to use Java 17 for Robolectric compatibility
tasks.withType<Test> {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(17))
    })
    
    // Additional test configuration
    maxHeapSize = "2g"
    
    // Show test output
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

android {
    namespace = "com.clickapps.crispify"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.clickapps.crispify"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Native build configuration
        ndk {
            // Only build for arm64-v8a as per PRD requirements
            abiFilters.add("arm64-v8a")
        }
        
        externalNativeBuild {
            cmake {
                // C++ flags for optimization and compatibility
                cppFlags.add("-std=c++17")
                cppFlags.add("-O3")
                cppFlags.add("-fno-exceptions")
                arguments.add("-DANDROID_STL=c++_shared")
            }
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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

    buildFeatures {
        viewBinding = false
        compose = true
    }
    
    // NDK version specification
    ndkVersion = "25.2.9519653"
    
    // CMake configuration
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Tokenizer (JTokkit) for CL100K_BASE token counting
    implementation("com.knuddels:jtokkit:1.1.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Unit Testing
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.ui.test.manifest)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Android Testing  
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    
    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
