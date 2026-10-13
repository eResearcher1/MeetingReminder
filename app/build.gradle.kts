plugins {
    id("com.android.application")
}

android {
    namespace = "com.xtraword.apps.meetminder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xtraword.apps.meetminder"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "1.2026.03.1"

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout-core:1.1.2")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation ("com.android.billingclient:billing:8.0.0")
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation ("androidx.work:work-runtime:2.9.1")

    // Optional: If you are using coroutines, add the following:
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Guava dependency if still facing ListenableFuture issues
    implementation ("com.google.guava:guava:31.1-android")
// Optional: Support for GCM on older devices (if needed)

}