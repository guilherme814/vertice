// Domínio puro (JVM): modelo e regras sem Android. Testa em segundos e não puxa o SDK.
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin { jvmToolchain(17) }

dependencies {
    api(libs.kotlinx.coroutines.core) // Flow nos contratos
    testImplementation(libs.junit)
}
