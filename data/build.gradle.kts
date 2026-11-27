// Dados e acesso ao sistema (LauncherApps, DataStore, cache de ícones). Sem UI.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vertice.launcher.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":domain"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui.graphics) // ImageBitmap do cache de ícones
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
