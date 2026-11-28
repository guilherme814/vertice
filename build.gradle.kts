plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
}

// ---------------------------------------------------------------------------------------------
// Guarda de arquitetura: as regras do docs/ARQUITETURA-10-FASES.md viram verificação automática.
// Roda no CI (e local: ./gradlew checkArchitecture). Falhou = a regra foi quebrada.
// ---------------------------------------------------------------------------------------------
tasks.register("checkArchitecture") {
    group = "verification"
    description = "Verifica dependências entre módulos e imports proibidos."
    doLast {
        // módulo -> módulos que ele PODE depender (o :app pode tudo)
        val allowed = mapOf(
            "domain" to emptySet(),
            "data" to setOf("domain"),
            "ui-core" to setOf("domain", "data"),
            "feature-home" to setOf("ui-core", "domain", "data"),
            "feature-drawer" to setOf("ui-core", "domain", "data"),
            "feature-settings" to setOf("ui-core", "domain", "data"),
            "feature-island" to setOf("ui-core", "domain", "data")
        )
        val errors = mutableListOf<String>()
        val dep = Regex("""project\(":([a-z-]+)"\)""")

        allowed.forEach { (module, ok) ->
            val build = file("$module/build.gradle.kts")
            if (!build.exists()) { errors += "módulo '$module' não encontrado"; return@forEach }
            dep.findAll(build.readText()).map { it.groupValues[1] }.filter { it !in ok }.forEach {
                errors += ":$module não pode depender de :$it (permitido: ${ok.ifEmpty { setOf("nada") }})"
            }
        }

        fun kotlinFiles(module: String) =
            fileTree(module) { include("src/**/*.kt") }.files

        // O domínio é Kotlin puro: nada de Android.
        kotlinFiles("domain").forEach { f ->
            f.readLines().forEachIndexed { i, line ->
                if (line.startsWith("import android.") || line.startsWith("import androidx.")) {
                    errors += "${f.relativeTo(projectDir)}:${i + 1} :domain importa Android (${line.trim()})"
                }
            }
        }

        // Telas e peças visuais falam com contratos (HomeActions), nunca com classes concretas de dados/ViewModel.
        val forbiddenInUi = listOf(
            "import com.vertice.launcher.LauncherViewModel",
            "import com.vertice.launcher.AppContainer",
            "import com.vertice.launcher.data.LauncherPrefs",
            "import com.vertice.launcher.data.AppRepository"
        )
        listOf("ui-core", "feature-home", "feature-drawer", "feature-settings", "feature-island").forEach { m ->
            kotlinFiles(m).forEach { f ->
                f.readLines().forEachIndexed { i, line ->
                    if (forbiddenInUi.any { line.startsWith(it) }) {
                        errors += "${f.relativeTo(projectDir)}:${i + 1} :$m importa classe concreta proibida (${line.trim()})"
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException("Regras de arquitetura quebradas:\n  - " + errors.joinToString("\n  - "))
        }
        println("Arquitetura OK (${allowed.size} módulos verificados).")
    }
}
