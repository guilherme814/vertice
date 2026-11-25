# ADR 0001 — Módulos e direção das dependências

**Decisão.** Sete módulos: `:domain` (Kotlin puro) ← `:data` (Android: sistema, DataStore, log) ← `:ui-core` (tema, peças, contrato `HomeActions`)
← `:feature-home` / `:feature-drawer` / `:feature-settings` ← `:app` (Activity, ViewModel, montagem).
Dependências só no sentido da seta; features não se conhecem. Verificado por `./gradlew checkArchitecture` no CI.

**Por quê.** Regras (layout, navegação, backup, categorias) testam na JVM em segundos; telas evoluem sozinhas;
trocar armazenamento ou fonte de apps não toca na interface. Padrão visto em Launcher3/Lawnchair/Kvaesitso.

**Custo.** Mais arquivos de build e tempo de configuração do Gradle. Aceito: o CI compensa e o app não fica maior por isso.
