plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.vertice.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vertice.launcher"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        // No CI o nome da versão ganha o número da execução e o commit (ex.: 1.0.0+ci42.abc1234):
        // aparece no log e na tela de diagnóstico, para saber exatamente qual APK está no aparelho.
        versionName = "1.0.0" + (System.getenv("GITHUB_RUN_NUMBER")?.let { n ->
            "+ci$n" + (System.getenv("GITHUB_SHA")?.take(7)?.let { ".$it" } ?: "")
        } ?: "")
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            // Leve de verdade: R8 + remoção de recursos não usados.
            isMinifyEnabled = true
            isShrinkResources = true
            // Assinado com a chave debug para poder instalar direto (sideload).
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":ui-core"))
    implementation(project(":feature-home"))
    implementation(project(":feature-drawer"))
    implementation(project(":feature-settings"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Instala o perfil de linha de base (baseline-prof.txt) no primeiro uso: abertura e Compose mais rápidos no release.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    debugImplementation(libs.compose.ui.tooling)
}
