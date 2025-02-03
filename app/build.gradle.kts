plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.sundalink.mapstracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sundalink.mapstracker"
        minSdk = 27
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    viewBinding {
        enable = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.firebase.vertexai)
    implementation(libs.androidx.espresso.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // OkHttp dan Retrofit
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Annotation dan legacy support
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // MQTT
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("com.github.hannesa2:paho.mqtt.android:3.3.5")

    // Socket.IO
    implementation("io.socket:socket.io-client:2.0.1")

    // Mapbox - gunakan hanya versi yang kompatibel
    implementation("com.mapbox.maps:android:10.14.1")
    implementation("com.mapbox.navigation:android:2.14.0")
    implementation("com.mapbox.navigation:ui-maps:2.14.0")

    // RecyclerView dan Lifecycle
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // ConstraintLayout dan AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Gson untuk JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
}
