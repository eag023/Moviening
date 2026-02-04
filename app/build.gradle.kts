plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.amgoapps.moviening"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amgoapps.moviening"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "5.0"

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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Plataforma BOM de Firebase
    implementation (platform("com.google.firebase:firebase-bom:33.1.0"))

    // Autenticación
    implementation ("com.google.firebase:firebase-auth")

    // Base de datos
    implementation ("com.google.firebase:firebase-firestore")

    // Para cargar imágenes de los pósters
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")

    // Retrofit y GSON
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Zoom en imágenes
    implementation("com.jsibbold:zoomage:1.3.1")

    // Inicio de sesión con Google
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation ("androidx.credentials:credentials:1.2.2")
    implementation ("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Usuarios con Firebase
    implementation("com.google.firebase:firebase-database:20.3.0")

    // Librería de Storage
    implementation("com.google.firebase:firebase-storage")
}