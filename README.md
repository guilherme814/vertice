# VÉRTICE LAUNCHER

Launcher Android nativa (Kotlin + Jetpack Compose), local-first: sem conta, sem backend, sem conexão obrigatória.
Meta de longo prazo: visual inspirado em HyperOS 4 / iOS 28, leve e muito personalizável.

## Roteiro por fases

| Fase | Foco | Estado |
|------|------|--------|
| 1 | **Construção**: launcher funcional, estrutura pronta para crescer | feita |
| 2 | Design: tema, ícones, vidro, tipografia, animações | próxima |
| 3 | Personalização: grade, dock, gestos, ícones, backup | — |
| 4 | Ajustes Premium e perfis de desempenho completos | — |

## Fase 1 — o que existe
- Registra-se como app **HOME** (e continua aparecendo como app comum).
- Lista de apps via `LauncherApps` (inclui perfil de trabalho) e **atualiza sozinha** ao instalar/remover — sem polling.
- Tela inicial: relógio, data, bateria (por broadcast) e dock persistente (até 5 apps).
- Gaveta (deslizar para cima ou botão) com busca por nome ou pacote, sem diferenciar acentos.
- Segurar um app: informações, colocar/tirar do dock, desinstalar.
- Botão Home fecha a gaveta; Voltar também.
- **Perfis de desempenho** (Ultra Lite / Equilibrado / Premium): hoje controlam animações e o teto do cache de ícones; o perfil inicial vem do hardware (`isLowRamDevice`). Futuras fases penduram blur, vidro e taxa de quadros no mesmo perfil.
- Cache de ícones limitado por **bytes**, para consumo de memória previsível.

## Estrutura
```
app/src/main/java/com/vertice/launcher/
  LauncherActivity.kt      tela inicial do aparelho (singleTask)
  LauncherViewModel.kt     junta apps + preferências em um estado para a UI
  core/                    PerformanceProfile
  data/                    AppRepository, AppEntry, IconCache, LauncherPrefs (DataStore)
  ui/                      HomeScreen, AppDrawer, AppTile, Theme
```
A tela só lê estado e dispara ações; dados e perfil não conhecem Compose. Quando um pedaço crescer
(ex.: ajustes), ele vira módulo Gradle sem reescrever o resto.

## Compatibilidade
Android 8.0 (API 26)+, JDK 17, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2025.01.00, compileSdk/targetSdk 35.

## Construir
O repositório não traz `gradlew`; o CI (`.github/workflows/build.yml`) instala o Gradle 8.10.2 e publica
o APK debug (e o release otimizado com R8) como artefatos a cada push.

Local: `gradle assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## Definir como tela inicial
Configurações → Aplicativos → Aplicativos padrão → App de início → **Vértice Launcher** (o caminho varia por fabricante).

## Limites honestos
Limpar cache ou encerrar processos de outros apps não é prometido: o Android restringe isso a apps comuns.
Recursos avançados devem usar APIs públicas ou integração opcional e consentida com Shizuku.
