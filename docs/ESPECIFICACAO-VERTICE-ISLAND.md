# Especificação — Vértice Island (v1)

Pedido de Luis (27/11/2026): uma ilha dinâmica própria da Vértice, com estilo HyperOS como tema padrão,
que não dependa de o fabricante oferecer uma. Decisão: pausar a expansão da Home/gaveta (fases 8+ do
`PLANO-20-FASES-PERSONALIZACAO.md`, depois da fase 7) e fazer a primeira versão da ilha antes da reformulação visual.

## 1. O que a v1 entrega

Uma cápsula preta no topo da tela, desenhada por cima de qualquer app, que aparece quando há algo relevante e some quando não há.

| Fonte | Como é detectada | Observação |
|---|---|---|
| Música | `MediaSessionManager.getActiveSessions` (usa o acesso a notificações) | título, artista, play/pausa, próxima/anterior |
| Notificações de apps | `NotificationListenerService` | só apps habilitados no Gerenciador da Ilha |
| Chamadas | notificação com `CATEGORY_CALL` e suas ações (atender/recusar) | evita a permissão de telefone |
| Cronômetro/timer | notificação com `EXTRA_SHOW_CHRONOMETER` / `CATEGORY_ALARM` | contagem local, sem polling |
| Carregamento | `ACTION_POWER_CONNECTED` / bateria já monitorada | efêmero (3 s) |
| Bluetooth | `ACTION_ACL_CONNECTED` | exige `BLUETOOTH_CONNECT` (API 31+); fica para a etapa 3 e é opcional |

Fora da v1 (limitações reais, não promessas): gravação de áudio de outros apps (o Android não expõe isso; só dá para reagir à
notificação do gravador, se existir), editor visual completo (Vértice Studio), posição "sobre" o recorte da câmera garantida em todos os aparelhos.

## 2. Arquitetura (encaixada nas regras atuais)

A proposta original sugeria um módulo `feature-dynamic-island` com `domain/data/service/ui` dentro. Isso quebraria
`checkArchitecture` (domínio puro, dependências só para baixo). A ilha é distribuída nas camadas que já existem, mais um módulo novo:

```
:domain            (Kotlin puro, com testes JVM)  pacote com.vertice.launcher.core.island
  IslandEvent.kt          eventos normalizados (Media, Notification, Call, Timer, Charging, Bluetooth)
  RawNotification.kt      retrato imutável de uma notificação (sem Android): pacote, categoria, ongoing, resumo de grupo,
                          cronômetro?, título, texto, tem ações de chamada?
  IslandClassifier.kt     RawNotification + IslandAppConfig -> IslandEvent? (regras: ignora resumo de grupo, silenciosas, apps desligados)
  IslandAppConfig.kt      por app: habilitado, modo (Temporária/Persistente), mostrar conteúdo
  IslandState.kt          Hidden | Compact(evento) | Expanded(evento)
  IslandArbiter.kt        máquina de estados pura: recebe eventos + tempo, devolve estado.
                          Prioridade: Chamada > Timer > Música > Notificação > Carregamento/Bluetooth.
                          Evento de prioridade menor espera numa fila (máx. 5); temporários expiram; deduplica por chave.
  IslandSettings.kt       ligada, estilo, tempo na tela, deslocamento vertical, largura, só em tela cheia? etc.

:data              IslandStore (interface no :domain) + implementação DataStore própria (arquivo separado do LauncherPrefs,
                   para o serviço não acordar o restante das preferências). Linhas novas no Backup.

:feature-island    NOVO módulo Android. Depende de :ui-core, :domain, :data (adicionar à tabela `allowed` do checkArchitecture)
  IslandNotificationListener.kt   NotificationListenerService; converte StatusBarNotification -> RawNotification -> classifier
  IslandMedia.kt                  MediaSessionManager + callbacks; produz IslandEvent.Media; controla transporte
  IslandOverlay.kt                dono da janela (WindowManager) + ComposeView; adiciona/remove conforme o estado
  ui/IslandCapsule.kt             cápsula compacta
  ui/IslandExpandedPanel.kt       painel expandido (controles, ações)
  ui/IslandSettingsScreen.kt      configurações + permissões guiadas
  ui/IslandAppSelector.kt         Gerenciador da Ilha (lista de apps com chave e modo)

:app               declara serviço e permissões no AndroidManifest, injeta no AppContainer; o painel de configurações da ilha é
                   aberto a partir do QuickSheet (nova aba "Ilha")
```

Decisão-chave: **o overlay vive dentro do `IslandNotificationListener`**. O sistema liga e mantém esse serviço enquanto o
acesso a notificações estiver concedido; assim não precisamos de serviço em primeiro plano nem de notificação fixa (Android 12+ restringe
iniciar serviço em segundo plano). A ilha não depende de a Home estar aberta.

## 3. Janela e desempenho (o Redmi A1 é o critério)

- `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL | FLAG_LAYOUT_IN_SCREEN`,
  gravidade topo-centro, `WRAP_CONTENT` (janela do tamanho da cápsula, nunca tela cheia).
- **Custo zero em repouso:** quando o estado é `Hidden`, a view é removida da janela (`removeView`); nada fica desenhando.
- Sem blur, sem camada offscreen, sem animação contínua. Animar só tamanho/alpha durante a transição (<= 250 ms).
  Perfil Ultra Lite: transição instantânea.
- Host Compose fora de Activity: `ComposeView` com `LifecycleOwner`, `SavedStateRegistryOwner` e `ViewModelStoreOwner` próprios
  (padrão conhecido), destruídos ao remover a janela.
- Posição: deslocamento vertical configurável (padrão alinhado ao recorte de câmera via `DisplayCutout`). A sobreposição fica
  acima dos apps e abaixo de partes críticas do sistema (teclado, às vezes barra de status). **Comportamento varia por fabricante;
  por isso o ajuste manual é requisito, não detalhe.**
- Não aparece sobre a tela de bloqueio nem durante gravação de tela/segurança (`FLAG_SECURE` em outros apps não é afetado, só não tentamos desenhar).

## 4. Permissões e primeira configuração

1. **Acesso a notificações** (`BIND_NOTIFICATION_LISTENER_SERVICE`, concedido em Configurações > Acesso a notificações).
2. **Sobreposição** (`SYSTEM_ALERT_WINDOW`, "Exibir sobre outros apps").
3. Opcional: `BLUETOOTH_CONNECT` (só se Bluetooth for ligado).
4. MIUI/HyperOS: o sistema pode matar o serviço. A tela de configurações mostra o estado ("ativa/parada"), explica
   "Início automático" e "Economia de bateria: sem restrições", e chama `requestRebind` quando o serviço cair.

Uma tela de checklist (3 itens, cada um com botão que abre a configuração certa) é a primeira coisa da aba "Ilha".

## 5. Privacidade (regras duras)

- Conteúdo de notificação fica **só em memória**, nunca em DataStore, backup ou log (`VLog` não recebe título/texto; só pacote e tipo).
- Por app: "mostrar conteúdo" desligado mostra só o ícone e "Nova mensagem".
- Notificações marcadas como privadas/sensíveis (`VISIBILITY_SECRET`) nunca mostram conteúdo.
- Tudo local; nenhuma rede.

## 6. Interação

- **Compacta:** ícone + informação essencial (música: capa pequena e onda estática; chamada: nome e tempo; timer: contagem).
- **Expandida (toque):** detalhes e ações (controles de mídia, atender/recusar, responder abre o app).
- **Temporária:** some após o tempo configurado (2–10 s). **Persistente:** fica enquanto a atividade existir (música tocando, chamada, timer).
- Arrastar para cima ou tocar no X dispensa só a notificação; atividade em andamento volta a ficar compacta, não é cancelada.
- Fechar o app de origem remove o evento (`onNotificationRemoved`).

## 7. Configurações da v1

Ilha ligada; app habilitado/desabilitado + modo por app (Gerenciador da Ilha); tempo da temporária; deslocamento vertical;
largura (compacta/média/larga); mostrar conteúdo; tema "HyperOS" (padrão) — o campo de tema já existe no modelo para o Vértice Studio
trocar depois sem migração. Tudo entra no backup (sem conteúdo de notificações).

## 8. Etapas (cada uma só fecha com CI verde)

| Etapa | Entrega | Estado |
|---|---|---|
| I1 Fundação | módulo `:feature-island` + regras no `checkArchitecture`; domínio completo (eventos, classificador, árbitro, config) com testes JVM; `IslandStore` + backup; manifest (listener + overlay); checklist de permissões; cápsula de teste ("Testar ilha") | **feita** |
| I2 Interface HyperOS | cápsula compacta/expandida, transições leves, Gerenciador da Ilha, aba "Ilha" no QuickSheet, deslocamento/largura | **feita** |
| I3 Integrações | música, notificações por app (WhatsApp, Instagram, Telegram), chamadas, timer, carregamento; Bluetooth opcional | **feita** |
| I4 Estabilidade | `requestRebind`, recuperação após reinício, limites de memória/fila, diagnóstico da ilha em `DiagnosticsTab`, teste nos 3 aparelhos | **feita** |

Critérios de aceite comuns: testes JVM do domínio verdes; `checkArchitecture` verde; APK release dentro do limite de tamanho;
ilha oculta = nenhuma janela de overlay no `dumpsys window`; log do A1 sem quadros novos acima de 100 ms causados pela ilha.

## 9. Impacto no plano de 20 fases

Fases 1–7 prontas. As fases 8–20 ficam **congeladas** até I4. Depois retomam; a fase 20 (Acabamento) e o futuro Vértice Studio
passam a incluir o tema da ilha. Nenhuma mudança na arquitetura da Home/gaveta é necessária.
