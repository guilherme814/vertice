# VÉRTICE LAUNCHER

Launcher Android nativa (Kotlin + Jetpack Compose), local-first: sem conta, sem backend, sem conexão obrigatória.
Meta de longo prazo: visual inspirado em HyperOS 4 / iOS 28, leve e muito personalizável.

## Roteiro por fases

Seguimos o *Plano de reestruturação da launcher* (PDF). Uma fase só começa quando a anterior cumpre o critério de aceite.
Ordem de prioridade: **base Android funcional → recursos → acabamento premium**.

| Fase | Foco | Estado |
|------|------|--------|
| 0 | Auditoria e estabilização | feita — ver [`docs/FASE-0-INVENTARIO.md`](docs/FASE-0-INVENTARIO.md) |
| 1 | Launcher mínima funcional (Home, apps, dock, grade básica, persistência) | implementada — aceite em [`docs/FASE-1-ACEITE.md`](docs/FASE-1-ACEITE.md) (falta confirmar reinício no aparelho) |
| 2 | Arquitetura modular (domínio, dados, módulos) | feita em 10 passos — ver [`docs/ARQUITETURA-10-FASES.md`](docs/ARQUITETURA-10-FASES.md) |
| 3 | Experiência de uso (busca, gestos, categorias, páginas, acessibilidade) | em andamento: busca (inclusive por gesto), páginas, categorias, TalkBack, robustez (idioma, apps suspensos, perfil de trabalho) feitos; faltam arrastar-e-soltar e ordenação da gaveta |
| 4 | Personalização premium (tema, wallpapers, ícones, widgets) | adiantada em parte: relógio, papéis de parede, grade e tamanho de ícones, dock em painel; faltam temas, formas de ícone, widgets |
| 5 | Otimização e compatibilidade (Redmi A1 e Xiaomi 12) | — |
| 6 | Qualidade e distribuição (assinatura, atualização, rollback) | — |

Os itens "adiantados" compilam, mas **não contam para o aceite** até as fases anteriores fecharem.

## O que existe hoje
- Registra-se como app **HOME** (e continua aparecendo como app comum); botão Home fecha a gaveta.
- Lista de apps via `LauncherApps` (inclui perfil de trabalho), atualizada sozinha ao instalar/remover — sem polling.
- Tela inicial com relógio, data, bateria (por broadcast), **grade 4x5 paginada** e dock (até 5 apps); posições persistidas.
- Segurar um app: Mover (toque na célula de destino), colocar/tirar da tela inicial ou do dock.
- Gaveta (deslizar para cima ou botão) com busca por nome ou pacote, sem diferenciar acentos; deslizar para baixo na Home abre a busca já focada.
- Personalizar: grade (colunas 3–6, linhas 4–7), tamanho dos ícones, nomes on/off, relógio, papéis de parede, backup e diagnóstico.
- Segurar um app: informações, dock, categoria, desinstalar.
- Perfis de desempenho (Ultra Lite / Equilibrado / Premium): hoje controlam animações e o teto do cache de ícones; o inicial vem do hardware.
- Adiantados: categorias na gaveta; relógio (4 estilos) e papéis de parede próprios, pela folha "Personalizar".

## Estrutura
```
app/     LauncherActivity, LauncherViewModel, AppContainer, ui/ (HomeScreen, AppDrawer, AppTile, QuickSheet, Diagnóstico)
ui-core/ tema, relógio, papel de parede, AppTile, AppIcon, CategoryChip, HomeUi/HomeActions
feature-home/ feature-drawer/ feature-settings/   uma tela por módulo
data/   AppRepository, AppEntry, IconCache, LauncherPrefs (DataStore), DevicePerformance, diag/ (log)
domain/ HomeLayout, SettingsStore/Prefs, PerformanceProfile, AppCategory, ClockStyle, Wallpaper, diag/  (Kotlin puro, com testes)
```
A tela só lê estado e dispara ações; dados e perfil não conhecem Compose. Quando um pedaço crescer
(ex.: ajustes), ele vira módulo Gradle sem reescrever o resto.

Módulos: `:app` (UI e ViewModel), `:data` (LauncherApps, DataStore, cache de ícones, leitura do hardware, log)
e `:domain` (Kotlin puro: layout da Home, preferências e seu contrato, perfis, categorias, relógio, papéis de parede — com testes de JVM).
Setas: `app -> feature-* -> ui-core -> data -> domain`; nunca no sentido contrário (features não se conhecem).

Diagnóstico e log (travamentos, crashes, memória): ver [`docs/DIAGNOSTICO.md`](docs/DIAGNOSTICO.md).

## Compatibilidade
Android 8.0 (API 26)+, JDK 17, AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2025.01.00, compileSdk/targetSdk 35.

## Construir
Gradle Wrapper incluído (Gradle 8.10.2, com SHA-256). O CI (`.github/workflows/build.yml`) valida o wrapper, compila e publica
o APK debug (e o release otimizado com R8) como artefatos a cada push.

Local: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

Atenção: o `applicationId` é `com.vertice.launcher` (o protótipo antigo era `com.vertice.game` e é outro app para o Android).

## Definir como tela inicial
Configurações → Aplicativos → Aplicativos padrão → App de início → **Vértice Launcher** (o caminho varia por fabricante).

## Limites honestos
Limpar cache ou encerrar processos de outros apps não é prometido: o Android restringe isso a apps comuns.
Recursos avançados devem usar APIs públicas ou integração opcional e consentida com Shizuku.
