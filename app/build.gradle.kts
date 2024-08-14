plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.testdoorbellapi28"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.testdoorbellapi28"
        minSdk = 28
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        pickFirst("META-INF/io.netty.versions.properties")
        pickFirst("META-INF/INDEX.LIST")

    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference:1.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("com.hivemq:hivemq-mqtt-client:1.2.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //  ML Kit Text Recognition v2 API (https://developers.google.com/ml-kit/vision/text-recognition/v2?hl=de)
    // To recognize Latin script
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    // To recognize Latin script (google play)
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
}