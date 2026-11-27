# Diagnóstico e gerador de log

Objetivo: saber **exatamente** o que acontece dentro da launcher (travamentos, conflitos, erros), sem adivinhar.
Tudo fica no aparelho; só sai quando você exporta.

## Como pegar o log
Segurar o fundo da Home (ou "✦ Personalizar") → aba **Diagnóstico**:
- **Marcar travamento**: toque assim que o aparelho travar/destravar. Carimba a hora; o log mostra o que o vigia viu antes e depois.
- **Salvar em Downloads**: grava `vertice-log-AAAAMMDD-HHMMSS.txt` em *Downloads* (Android 10+).
- **Compartilhar**: abre o seletor do Android (WhatsApp, Drive, e-mail...).
- **Registro detalhado**: liga/desliga as linhas de depuração (ligado por padrão).
Mande o arquivo a cada versão testada. Contém nomes de pacotes dos apps abertos.

## O que é registrado
| Tag | O que mostra |
|---|---|
| `Sistema` | versão (no CI: `1.0.0+ci<n>.<commit>`), debug/release, Android, modelo, RAM, tela, se a Vértice é a Home padrão |
| `Saída` | **por que o processo anterior morreu** (Android 11+): ANR, POUCA_MEMORIA, CRASH, SINAL... Dado-chave p/ separar "travou" de "o sistema matou" |
| `Abertura` | tempo até o primeiro quadro e até a Home estar pronta, desde o início do processo |
| `Travamento` | thread principal parada >1 s, **com a pilha de onde ela está presa**, e quanto durou |
| `Quadros` | resumo a cada ~5 s (lentos >32 ms, travados >100 ms, congelados >700 ms) e linha por quadro ≥100 ms com a divisão por fase: layout, desenho, GPU... |
| `Memória` | heap, memória nativa, memória livre do sistema; avisos `onTrimMemory`/`onLowMemory` |
| `Ciclo` | onCreate/onResume/onPause/onDestroy com tempo desde o início do processo |
| `Apps` | tempo de carga da lista, eventos de instalar/remover, falhas ao abrir app |
| `Ícones` | acertos/faltas do cache, ícones lentos, falhas de decodificação |
| `Layout` | cada edição da grade (colocar, mover, tirar) e seu resultado |
| `Prefs` | primeira execução, falha ao ler/gravar preferências |
| `StrictMode` | disco/rede na thread principal e vazamentos (cada origem uma vez) |
| `Marca` | seus toques em "Marcar travamento" |
| `Crash` | exceção não tratada: pilha completa + últimas 150 linhas (também em `crash-*.txt`) |

Formato: `2026-11-24 13:41:02.123 W/Travamento [vertice-watchdog] mensagem`. Mensagem em várias linhas: continuação indentada.

## Como ler um travamento
1. Procure `Marca` ou `Travamento` na hora em que travou.
2. A pilha em `Travamento` mostra o método onde a thread principal estava parada (Compose, binder, disco...).
3. `Quadros ... Quadro lento` diz qual fase estourou (layout x desenho x GPU).
4. `Saída` na sessão seguinte diz se o sistema matou o processo (POUCA_MEMORIA) ou se foi ANR/crash.

## Custos e limites
- Escrita em thread própria, fila limitada, rotação (512 KB por arquivo, 4 arquivos + atuais): poucos MB no máximo.
- O vigia e a medição de quadros só rodam com a launcher visível (onResume→onPause).
- Código: `domain/.../diag` (formato, anel, estatística de quadros — com testes) e `data/.../diag` (Android).
