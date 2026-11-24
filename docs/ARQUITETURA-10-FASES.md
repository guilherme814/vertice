# Arquitetura da Vértice — análise e plano em 10 fases

Esta é a conclusão da **Fase 2 do PDF (arquitetura modular)**, dividida em 10 passos pequenos, feitos **um por vez**.
Cada passo só começa depois que o anterior compila, passa nos testes e roda no CI.

## 1. Diagnóstico do repositório hoje
| Achado | Efeito |
|---|---|
| `LauncherViewModel` cria `LauncherPrefs` e `AppRepository` direto | impossível testar sem Android; trocar armazenamento mexe na UI |
| A montagem do `HomeUi` (sanitize, categorias, dock/grade) mora no ViewModel | regra de negócio presa à camada Android |
| `AppEntry` mistura dado (rótulo, chave) com alça do sistema (`LauncherActivityInfo`) | o domínio não consegue falar de "app" sem Android |
| Telas Composable decidem estado de navegação (`drawerOpen`, `sheetOpen`, `moving`) | sem máquina de estados única; difícil de testar e de restaurar |
| Tema e componentes comuns (`AppTile`, `CategoryChip`) dentro do `:app` | impede separar telas em módulos |
| Sem regra automática de dependência entre módulos | a seta `app → data → domain` só vale por disciplina |
| Testes só no domínio | o ViewModel e a UI não têm rede de proteção |

Já resolvido: layout como dado (`LayoutOps`), módulos `:domain` (Kotlin puro) e `:data` (Android), contrato `LayoutStore`,
log/diagnóstico (`docs/DIAGNOSTICO.md`).

## 2. O que as outras launchers ensinam (resumo; detalhes em `ANALISE-LAUNCHERS.md`)
- **Launcher3 (AOSP):** modelo (`ItemInfo` + banco) separado das views; um *loader* que carrega em segundo plano e entrega o estado pronto; `StateManager` para telas (Home, Todos os apps, Overview). → nossos passos 2, 3 e 5.
- **Lawnchair:** camada de preferências tipada e observável, desacoplada das telas; recursos por *feature flag*. → passos 7 e 10.
- **Kvaesitso:** módulos por responsabilidade (`data`, `ui`, `services`, `core`), repositórios atrás de interfaces, injeção explícita. → passos 1, 3, 4, 6, 8.
- **Regra comum:** a tela só **observa estado** e **envia intenções**; nada de regra dentro de Composable.

## 3. Os 10 passos
| # | Passo | Resultado verificável |
|---|---|---|
| 1 | **Domínio puro e contratos de dados:** `PerformanceProfile`, `AppCategory`, `Prefs` e a interface `SettingsStore` vão para `:domain` | `:domain` sem nenhum import Android; testes novos; VM usa a interface |
| 2 | **Modelo de app sem Android:** `AppModel` (chave, pacote, rótulo, categoria declarada) no domínio; `AppEntry` passa a envolvê-lo | regras de app testáveis na JVM |
| 3 | **Regra de montagem da Home no domínio:** função pura `HomeState.from(apps, prefs)` (sanitize, dock, grade, categorias) | ViewModel só conecta fluxos; testes cobrem a regra |
| 4 | **Injeção manual de dependências:** `AppContainer` criado em `VerticeApp`; `ViewModelProvider.Factory` | ViewModel recebe interfaces; testes com `InMemorySettings` |
| 5 | **Estado e intenções de UI:** `HomeIntent` + máquina de estados das telas (Home, Gaveta, Folha, Mover) fora do Composable | navegação testável; voltar/Home previsíveis |
| 6 | **Módulo `:ui-core`:** tema, cores, `AppTile`, `AppIcon`, `CategoryChip` | telas não dependem do `:app` |
| 7 | **Módulos de funcionalidade:** `:feature-home`, `:feature-drawer`, `:feature-settings` | cada tela compila e evolui sozinha |
| 8 | **Plataforma atrás de interfaces:** `AppCatalog`, `AppLauncher`, `PackageEvents`, `BatteryStatus` em `:data`, com fakes | tudo que fala com o Android é substituível |
| 9 | **Preferências versionadas:** esquema com número de versão, migrações testadas, backup/restauração | atualizar o app nunca perde posições |
| 10 | **Guardas automáticas:** verificação de dependências entre módulos no CI, lint, limite de tamanho do APK, ADRs curtos | a arquitetura se protege sozinha |

## 4. Andamento
| # | Estado |
|---|---|
| 1 | **feito** — ver abaixo |
| 2 | **feito** — `AppModel` (domínio) + `AppEntry` envolve o modelo |
| 3–10 | pendentes (um de cada vez) |

### Passo 1 — o que mudou
- `:domain` agora contém `PerformanceProfile`, `AppCategory` (com as constantes de `ApplicationInfo` copiadas e travadas por teste), `Prefs`, `SettingsStore`.
- `:data` mantém só o que fala com Android: `LauncherPrefs` (DataStore, implementa `SettingsStore`) e `DevicePerformance` (lê `isLowRamDevice`).
- `LauncherViewModel` depende de `SettingsStore`, não da classe concreta.
- Testes novos: categorias, perfis, contrato de armazenamento (versão em memória, que serve de fake nos próximos passos).
