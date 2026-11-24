# Como launchers de verdade são construídos — análise e consequências para a Vértice

Fontes lidas: [Launcher3 (AOSP internals)](https://aospbooks.github.io/aosp-internal-book/48-launcher3/),
[glossário de arquitetura do Lawnchair](https://docs.lawnchair.app/developers/architecture/glossary),
[guia oficial de widget host](https://developer.android.com/guide/topics/appwidgets/host),
[referência do `LauncherApps`](https://developer.android.com/reference/android/content/pm/LauncherApps) (a página veio truncada: o que
vai além dela está marcado como *conhecimento geral, a verificar*) e o [Kvaesitso](https://github.com/MM2-0/Kvaesitso)
(launcher em Kotlin + Compose, GPL-3.0).

## 1. O desenho que se repete

| Ideia | Como aparece em Launcher3 / Lawnchair | Na Vértice hoje |
|---|---|---|
| Modelo separado das telas | `LauncherModel` guarda o estado; `LoaderTask` carrega em thread de fundo; o resultado chega à UI por `Callbacks` | **Alinhado**: `AppRepository` + `LauncherViewModel` + `StateFlow` |
| Item com posição explícita | `ItemInfo` tem tipo, *container* (desktop, hotseat, gaveta), tela, `cellX`, `cellY`, tamanho | **Lacuna**: só o dock é salvo, como lista ordenada; não existe posição |
| Grade como dado | tamanhos de grade em XML por aparelho (`IDP`/`DeviceProfile`); um banco por tamanho de grade | **Lacuna**: ainda não há grade |
| Máquina de estados | `LauncherState` (NORMAL, ALL_APPS, …) + `StateManager` | **Lacuna**: duas variáveis booleanas na Activity (`drawerOpen`, `sheetOpen`) |
| Escritas serializadas | `ModelWriter` numa única thread de modelo | **Alinhado**: o DataStore já serializa as escritas |
| Trabalho preso ao ciclo de vida | widgets só atualizam com a tela visível | **Parcial**: `collectAsStateWithLifecycle`; widgets ainda não existem |
| Preferências em camada própria | Lawnchair usa `PreferenceManager2` + recarga explícita; extensões (busca, feed) por *providers* | **Alinhado**: `LauncherPrefs` isolado |
| Escopo deliberado | busca, tema, pastas, recentes são subsistemas grandes | Já estamos fora de ordem em tema/relógio (ver inventário) |

O Kvaesitso mostra que um launcher Compose cresce bem com módulos (`app`, `core`, `data`, `services`, `libs`) e um SDK de plugins
para extensões — direção compatível com a Fase 2 do plano.

## 2. O que isso muda, fase a fase

**Fase 1 (aceite pendente: grade + posições persistidas).**
Modelar cada item da Home como `LayoutItem(key, container, página, célula x, célula y)` em vez de "lista ordenada". Motivo: com lista,
remover um app desloca todos os outros; com coordenadas, a grade mantém o que o usuário arrumou e aceita células vazias.
`container` ∈ {Home, Dock}. Persistência continua no DataStore (centenas de itens, sem consultas), como o plano pede
("Room só onde necessário"). A grade (colunas × linhas) vem de um perfil do aparelho, não de constantes.

**Fase 2 (arquitetura modular).**
Trocar `drawerOpen`/`sheetOpen` por um estado único (`Home`, `Drawer`, `Personalizar`) — evita combinações impossíveis.
Extrair `launcher-domain` com os casos de uso (`GetApps`, `ReorderItems`), e deixar `core-platform` com `LauncherApps`, `PackageManager`, widgets.

**Fase 3.** Tratar `onPackagesSuspended/Unsuspended` e recarregar rótulos ao mudar o idioma (hoje o nome do app é lido uma vez; ver §3).
Atalhos de app (`ShortcutQuery`) *(conhecimento geral, a verificar)* só são entregues ao launcher padrão — há `hasShortcutHostPermission()` para checar.

**Fase 4 — widgets.** Pelo guia oficial:
- `AppWidgetHost` com ID fixo e único no pacote; `AppWidgetManager` para vincular;
- `BIND_APPWIDGET` no manifest **não basta**: chamar `bindAppWidgetIdIfAllowed()` e, se der `false`, abrir o diálogo do sistema (`ACTION_APPWIDGET_BIND`);
- `allocateAppWidgetId()` ao adicionar, **persistir o ID junto com a posição**, `deleteAppWidgetId()` ao remover (vazar ID é erro comum);
- abrir a atividade de configuração do widget quando houver, e tratar o caso de ela nunca retornar;
- `startListening()` com a tela visível e `stopListening()` fora dela *(o guia não detalha; vem da referência do `AppWidgetHost`)*.
Isso reforça por que a posição precisa ser dado persistido desde a Fase 1: o widget depende do mesmo registro.

**Fase 5.** O `HomeUi` recalcula as categorias de todos os apps a cada mudança de qualquer preferência (inclusive papel de parede).
Com 39 apps é irrelevante, mas é o tipo de coisa a medir em Redmi A1 antes de mexer — o plano exige medição, não palpite.

## 3. Pontos do nosso código que a análise tornou suspeitos (não corrigidos ainda)

1. **Rótulos presos no idioma de abertura.** `AppRepository` lê `info.label` na carga. Se o idioma do sistema mudar com a launcher aberta
   (a Activity declara `configChanges=locale`), os nomes ficam desatualizados até outra mudança de pacote. Launcher3 recarrega nesse caso.
2. **Suspensão de apps** (`onPackagesSuspended`) não é tratada: app suspenso continua abrindo normalmente na lista.
3. **Alternar o dock lê e grava em duas etapas** (`ui.value` e depois `setDock`): dois toques rápidos podem perder um. O correto é transformar o valor dentro do `edit {}` do DataStore.
4. **Perfil de trabalho bloqueado**: não há tratamento de desbloqueio de usuário; apps do perfil podem aparecer/sumir sem recarga.

Nenhum desses itens foi observado em aparelho: são riscos apontados pela comparação com o modo como launchers maduros se comportam.

## 4. Evidência de que algo funciona

A lista de apps usa `LauncherApps` **sem** `QUERY_ALL_PACKAGES`; a gravação de tela do usuário mostrou 39 apps listados corretamente em Android real.
Logo, para o núcleo de uma launcher, essa permissão ampla não é necessária (coerente com a nota de compatibilidade do plano).
