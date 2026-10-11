# VÉRTICE

App de conversas brasileiro — dark-first, Compose Material 3, arquitetura multi-módulo.

## Marca
- `branding/vertice-mark.svg` / `.png` — marca primária (V de traço único com nó final)
- `branding/vertice-lockup.svg` — marca + wordmark
- Ícone do launcher: `app/src/main/res/drawable/ic_launcher_foreground.xml` (geometria idêntica à marca)
- Cores/tipografia: `core-ui/src/main/kotlin/com/vertice/core/ui/theme/`

## Módulos
- `app` — shell, navegação, bottom bar
- `core` — utilidades (AppResult, Dispatchers)
- `core-ui` — tema e componentes (VerticeButton, VerticeCard, ícones, marca)
- `core-database` — DataStore (`vertice_prefs`)
- `core-network` — cliente Ktor + observador de conectividade
- `core-security` — segurança de tela
- `feature-auth`, `feature-chat`, `feature-communities`, `feature-moments`,
  `feature-profile`, `feature-settings`, `feature-calls`, `feature-screen-share`, `feature-media`

## Build
Requer JDK 17 + Android SDK 35.

```bash
gradle assembleDebug
```

O workflow `.github/workflows/build.yml` compila e publica o APK debug a cada push na `main`.

## Instalação no aparelho
```bash
gradle installDebug
# ou
adb install app/build/outputs/apk/debug/app-debug.apk
```
O manifest usa `android:installLocation="auto"`: instala no interno por padrão e
permite mover para o cartão quando o aparelho suporta.
