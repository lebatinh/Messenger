plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
}

android {
    namespace = "com.lebatinh.messenger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lebatinh.messenger"
        minSdk = 26
        targetSdk = 34
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
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }
}
configurations.all {
    resolutionStrategy {
        force("org.apache.httpcomponents:httpclient:4.5.13")
        force("org.apache.httpcomponents:httpcore:4.4.15")
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

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Convert dp,sp to sdp, ssp
    implementation(libs.sdp.android)
    implementation(libs.ssp.android)

    // Lottie
    implementation(libs.lottie)

    // Datastore
    implementation(libs.androidx.datastore.preferences)

    //Coroutine
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // ViewModel and LiveData components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Animation transition with multi fragment with navigation
    implementation(libs.androidx.transition)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)

    // Mail
    implementation(libs.android.mail)
    implementation(libs.android.activation)

    // Google play service
    implementation(libs.play.services.base)
    implementation(libs.play.services.auth)

    // Gson
    implementation(libs.gson)

    // Storage image & video with Cloudinary
    implementation(libs.cloudinary.android)

    // Glide
    implementation(libs.glide)
    ksp(libs.ksp)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Image Crop
    implementation(libs.ucrop)

    // ViewPager2
    implementation(libs.androidx.viewpager2)

    // ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)

    // Media Picker load image/video from gallery (can use other type as file, audio)
    implementation(libs.picker)

    // Dots Indicator
    implementation(libs.dotsindicator)

    // Emoji
    implementation(libs.emoji.google)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.extensions)

    // Google Auth Library
    implementation(libs.google.auth.library.oauth2.http) {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
    }

    // OkHttp
    implementation(libs.okhttp)
}