# Otimização a partir do log de 25/11 (build DEBUG, Xiaomi 12)

Fontes: `vertice-log-20261125-201922.txt` e o PDF "Plano de Correção em 10 Fases".
Regra: só entra o que o log comprova; o resto fica registrado com o motivo.

| Item do plano | O que o log mostra | Decisão |
|---|---|---|
| F3 – disco na aba Diagnósticos | 3 `DiskReadViolation` em `DiagnosticsTab`→`VLog.totalBytes` | **Feito**: medição em `Dispatchers.IO` |
| F8 – `onNewIntent` duplicado | 9 pares no mesmo milissegundo | **Feito**: segundo evento em 150 ms é ignorado |
| F5 – "Home pronta: 0 apps" | após `POUCA_MEMORIA` o processo volta e a Home sai vazia | **Feito**: `loading` só termina com preferências **e** apps |
| F1/F2 – recomposição na gaveta | quadros com `anim=60–190ms` ao rolar/abrir | **Feito**: posição do ícone fora do estado (rolar recompunha os 90 ícones) e `DropdownMenu` só quando aberto |
| F6 – ícones lentos (40–83 ms) | decodificação sob o dedo | **Feito**: `IconCache.prewarm` dos 24 primeiros, 2 em paralelo, após o 1º quadro |
| F10 – release/baseline | log é de build DEBUG | **Feito**: `baseline-prof.txt` + `profileinstaller`; **medir no APK release** |
| F9 – fontes MIUI | violações vêm de `miui.util.*`, sem linha do app | Filtradas do log (não há o que corrigir) |
| F4 – trabalho no `onResume` | `onResume` do app não faz trabalho; atraso alto vem da CPU ocupada pelo jogo | Sem mudança |
| F7 – flags da janela | alterna só ao trocar o papel de parede (1 vez no log) | Sem mudança |

Observações do log:
- `POUCA_MEMORIA` mata o processo em segundo plano (importância 400, cacheado) com um jogo pesado aberto: é o sistema, não vazamento (heap 6–27 MB).
- Primeiro quadro: 343 ms, `layout=271` (debug + Compose frio). O perfil de linha de base ataca exatamente isso.
- **Próxima medição:** instalar o APK *release* do CI e repetir o uso; comparar `quadros lentos` e `travados` no log.
