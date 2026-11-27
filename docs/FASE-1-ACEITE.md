# Fase 1 — critérios de aceite

| Critério (PDF) | Situação | Como foi verificado |
|---|---|---|
| Registra-se como Home e abre | ok | Visto no aparelho (vídeo do Luis) |
| Lista de apps, abrir app | ok | Idem (39 apps) |
| Dock | ok | Idem |
| Grade básica na Home (4x5, páginas) | implementada | CI compila; lógica coberta por testes de JVM (`HomeLayoutTest`) |
| Posições persistidas (favoritos, grade) | implementada | Layout salvo no DataStore (`layout_v2`), migra o dock antigo; testes de codec e `sanitize` |
| Edição sem condição de corrida | corrigida | `editLayout` lê-transforma-grava numa transação só |
| Sobrevive a reinício | **pendente no aparelho** | Ver roteiro abaixo |

## Roteiro no aparelho (Luis)
**Teste com o APK `release` (artefato `vertice-launcher-release-apk`), não com o debug.** O debug do Compose
roda sem otimização e é muito mais lento, principalmente em aparelhos de 2 GB; ele não representa a launcher real.
1. Instale o APK novo (se pedir, desinstale a versão anterior — os dados do dock antigo serão refeitos).
2. Gaveta → segurar um app → "Colocar na tela inicial". Repita com 3–4 apps.
3. Segurar um app na grade → "Mover" → tocar outra célula (vazia = move; ocupada = troca). Tocar na célula de uma página nova (aparece uma extra durante o movimento).
4. Segurar → "Tirar da tela inicial"; "Colocar no dock" / "Tirar do dock".
5. Reinicie o aparelho. Confirme: a Vértice continua como Home e tudo está no mesmo lugar.
6. Instale/desinstale um app qualquer: o resto da grade não deve se mexer.

## Limites conhecidos (ficam para as fases seguintes)
- Mover é por toque (arrastar e soltar entra na Fase 3).
- Sem pastas, sem widgets (Fase 4).

## Travamentos relatados no teste de reinício
Reduzido o trabalho desnecessário: a lista de apps só redesenha a tela quando algo realmente muda
(antes, qualquer recarga criava objetos "novos"), o estado da Home é montado fora da thread da interface e a
classificação por categoria é calculada uma vez por app. A causa exata só se confirma medindo no aparelho:
anote o modelo, se o APK era debug ou release e quando trava (logo após ligar, ao deslizar páginas, ao abrir a gaveta).
