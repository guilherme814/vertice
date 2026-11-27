# Fase 0 — Auditoria e estabilização

Referência: *Plano de reestruturação da launcher* (PDF, nov/2026). Este documento é a entrega da Fase 0
("inventário técnico documentado; projeto compila em ambiente limpo; nenhuma mudança destrutiva sem justificativa").

Base: branch `claude/vertice-launcher`. Não confundir com a `main`, que ainda contém o protótipo descrito no plano.

## 1. Inventário de arquivos

| Arquivo | Responsabilidade |
|---|---|
| `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` | Build. Um único módulo (`:app`); catálogo só com o que é usado. |
| `gradlew`, `gradle/wrapper/*` | Gradle Wrapper 8.10.2 (com SHA-256 da distribuição). |
| `.github/workflows/build.yml` | CI: valida o wrapper, compila debug e release (R8), publica os APKs como artefatos. |
| `app/build.gradle.kts` | `com.vertice.launcher`, minSdk 26, target/compile 35, R8 + remoção de recursos no release. |
| `app/src/main/AndroidManifest.xml` | `HOME`+`DEFAULT` e `LAUNCHER`, `singleTask`, `queries` mínimas, `REQUEST_DELETE_PACKAGES`. |
| `LauncherActivity.kt` | Activity HOME, ciclo de vida, botão Home/Voltar, composição das telas. |
| `LauncherViewModel.kt` | Junta apps + preferências em um estado (`HomeUi`) para a UI. |
| `data/AppRepository.kt` | Apps via `LauncherApps` (inclui perfil de trabalho), atualização ao vivo por callback. |
| `data/AppEntry.kt`, `data/IconCache.kt` | Modelo de app; cache de ícones limitado por bytes. |
| `data/LauncherPrefs.kt` | Preferências (DataStore): dock, perfil, categorias, relógio, papel de parede. |
| `core/*` | Regras sem Android UI: `PerformanceProfile`, `AppCategory`, `ClockStyle`, `Wallpaper`. |
| `ui/*` | Compose: `HomeScreen`, `AppDrawer`, `AppTile`, `Clock`, `QuickSheet`, `WallpaperBackground`, `Theme`. |
| `res/*`, `branding/*` | Ícone adaptativo (com monocromático), cores, temas, marca em SVG. |

Total: 17 arquivos Kotlin, ~1,7 mil linhas.

## 2. Módulos

O `settings.gradle.kts` herdado declarava **15 módulos** (`:core`, `:core-ui`, `:feature-chat`, …) que **não existem** no repositório.
Isso quebraria qualquer build limpo. Decisão: ficar com `:app` e modularizar na **Fase 2** do plano,
depois que a base for verificada — como o próprio plano recomenda ("não é obrigação criar um módulo por pasta desde o primeiro dia").
Os pacotes atuais (`core`, `data`, `ui`) já seguem a divisão de responsabilidades planejada, para facilitar a extração.

## 3. Dependências (todas em uso)

Compose (BOM 2025.01.00: ui, graphics, foundation, material3), activity-compose 1.10.0, lifecycle-runtime-compose e
viewmodel 2.8.7, DataStore Preferences 1.1.1, coroutines-android 1.8.0. AGP 8.7.3, Kotlin 2.0.21.
Removidas: Hilt, Room, Ktor, Coil, Tink, Navigation, WorkManager, serialização (nenhuma era usada).

## 4. Permissões e privacidade

| Item | Uso |
|---|---|
| `REQUEST_DELETE_PACKAGES` | Desinstalar app pelo menu (Android 9+ exige). |
| `<queries>` (LAUNCHER, DIAL, APP_MESSAGING, VIEW https, IMAGE_CAPTURE) | Só para achar apps padrão ao montar o dock inicial. |
| *Sem* `QUERY_ALL_PACKAGES`, *sem* `INTERNET` | A lista vem do `LauncherApps`; o app não acessa a rede. |

## 5. Identidade e estratégia de migração

| | Protótipo (`main`) | Atual |
|---|---|---|
| Nome do produto | "NEXUS" (raiz do Gradle) / "Vértice" | Vértice Launcher |
| `applicationId` / namespace | `com.vertice.game` | `com.vertice.launcher` |
| README | descrevia app de conversas | descreve a launcher |

**Migração.** Mudar o `applicationId` cria, para o Android, um **aplicativo diferente**: quem tiver o protótipo antigo instalado
não receberá "atualização" — os dois coexistem. Como o protótipo não gravava nenhum dado, não há o que migrar.
Procedimento: instalar a nova, definir como Home, desinstalar a antiga. **O `applicationId` agora fica congelado**: mudar de novo
perderia dados e a possibilidade de atualizar por cima.

## 6. O que foi aproveitado do protótipo e o que foi descartado

Aproveitado: paleta (Ink/Mint), relógio e data, indicador de bateria, busca também por nome do pacote, textos em português.
Descartado: lista via `queryIntentActivities` + `Drawable` dentro de `AndroidView` (trocada por `LauncherApps` + cache),
"favoritos" como `take(5)` (não persistia), cartão promocional sem função, filtro HOME duplicado sem `singleTask`.

## 7. Itens adiantados fora da ordem do plano

Para ser transparente: ao longo da conversa entraram itens de fases posteriores.

| Item | Fase do plano | Situação |
|---|---|---|
| Categorias de apps na gaveta | 3 | no código; **não conta para o aceite** |
| Relógio com 4 estilos, papéis de parede próprios, folha "Personalizar" | 4 | no código; **não conta para o aceite** |
| Atualizador por GitHub Releases | 6 | **estacionado fora do repositório** (não está em nenhum commit) |

Eles compilam no CI, mas só passam a valer depois que as Fases 1–3 forem aceitas.

## 8. Estado de verificação (honesto)

- Compila em ambiente limpo: **sim**, no CI (runner novo do GitHub) — debug e release.
- Testado em aparelho: **parcialmente** — gravação de tela da Fase 1 mostrou a launcher funcionando (Home, gaveta, dock, bateria).
  Categorias, relógio e papéis de parede ainda **não foram vistos em aparelho**.
- Testes automatizados: **nenhum ainda**. Medições de desempenho: **nenhuma**.
- Nunca compilado localmente (o ambiente de desenvolvimento não tem Android SDK): quem compila é o CI.

## 9. Lacunas conhecidas em relação à Fase 1 (aceite)

Critério: *"aparece na escolha de Home, pode ser definida como padrão, sobrevive a reinicializações e abre os apps instalados."*

- [x] Aparece na escolha de Home e abre apps (visto em vídeo).
- [ ] Sobrevive a reinicializações — **precisa ser verificado no aparelho** (reiniciar e conferir dock e Home padrão).
- [ ] **Grade básica na tela inicial** — a Home tem relógio e dock, mas ainda não tem grade de apps.
- [ ] **Persistir posições** — o dock persiste; as posições na grade da Home ainda não existem.
