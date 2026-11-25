# Plano em 10 fases — estrutura final, design e funcionalidades exclusivas

Segue o plano do PDF (fases 3–4 e adiante) depois dos 10 passos de arquitetura (`ARQUITETURA-10-FASES.md`).
Uma fase por vez; cada uma só fecha com CI verde (testes + regras de arquitetura + APK debug/release).

| # | Fase | Entrega | Estado |
|---|---|---|---|
| 1 | **Estrutura final** | `HomeActions` dividido em contratos menores; tokens de design (espaço, raio, tipografia) em `:ui-core` | **feita** |
| 2 | **Sistema de temas** | cor de destaque (8 + cor do sistema no Android 12+), claro/escuro/automático, intensidade do vidro; persistido, no backup | — |
| 3 | **Estilo de ícones** | forma (círculo, squircle, quadrado arredondado), sombra, borda; um só lugar aplica em Home, dock e gaveta | — |
| 4 | **Gaveta nova** | ordenar (A–Z, recentes), seções por letra com índice lateral, ocultar apps | — |
| 5 | **Arrastar e soltar** | mover na grade e no dock arrastando, com sombra e destino destacado (substitui o "toque na célula") | — |
| 6 | **Pastas** | modelo no domínio, criar/renomear/abrir, na grade e no dock | — |
| 7 | **Exclusivo I — Foco e sugestões** | Modo Foco (esconde apps escolhidos com um toque), "mais usados" e "recentes" calculados só no aparelho | — |
| 8 | **Exclusivo II — Gestos e atalhos** | gestos configuráveis (duplo toque, deslizar), notificações, atalhos de app no menu de segurar | — |
| 9 | **Widgets** | relógio próprio + widgets do sistema (AppWidgetHost) na grade | — |
| 10 | **Acabamento** | transições, paralaxe do papel de parede, primeira execução guiada, documentação final | — |

Regras de trabalho: privacidade local (nada de rede), respeitar os perfis de desempenho (Ultra Lite sem efeitos caros),
cada regra nova nasce no domínio com testes.
