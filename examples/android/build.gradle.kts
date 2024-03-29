plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.connectrpc.examples.android"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "com.connectrpc.examples.android"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.android.multidex)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.android.material)

    implementation(project(":okhttp"))
    implementation(project(":examples:generated-google-javalite"))
    implementation(libs.okhttp.core)

    // Flipper Plugin
    // https://github.com/facebook/flipper
    debugImplementation("com.facebook.flipper:flipper:0.236.0")
    debugImplementation("com.facebook.soloader:soloader:0.10.5")
    releaseImplementation("com.facebook.flipper:flipper-noop:0.236.0")
    debugImplementation("com.facebook.flipper:flipper-network-plugin:0.236.0")
}
