# Plano em 10 fases — estrutura final, design e funcionalidades exclusivas

Segue o plano do PDF (fases 3–4 e adiante) depois dos 10 passos de arquitetura (`ARQUITETURA-10-FASES.md`).
Uma fase por vez; cada uma só fecha com CI verde (testes + regras de arquitetura + APK debug/release).

| # | Fase | Entrega | Estado |
|---|---|---|---|
| 1 | **Estrutura final** | `HomeActions` dividido em contratos menores; tokens de design (espaço, raio, tipografia) em `:ui-core` | **feita** |
| 2 | **Sistema de temas** | cor de destaque (8 + cor do sistema no Android 12+) e intensidade do vidro; persistido, no backup. (Modo claro fica para depois: exige separar cores "sobre o papel de parede" das superfícies) | **feita** |
| 3 | **Estilo de ícones** | forma (círculo, squircle, arredondado, quadrado), sombra; ícone adaptativo desenhado sem máscara e recortado na forma escolhida; aplica em Home, dock e gaveta | **feita** |
| 4 | **Gaveta nova** | ordenar (A–Z, Novos), seções por letra com índice lateral, ocultar apps (modo "Ocultos" para reexibir) | **feita** |
| 5 | **Arrastar e soltar** | mover na grade e no dock arrastando, com sombra e destino destacado (substitui o "toque na célula") | **feita** |
| 6a | **Liquid glass** | vidro leitoso com brilho, borda de luz e reflexo (rico no Premium/Equilibrado, simples no Ultra Lite), sem desfoque em tempo real; papel de parede pintado uma vez em textura | **feita** |
| 6b | **Pastas** | modelo no domínio, criar/renomear/abrir, na grade e no dock | — |
| 7 | **Exclusivo I — Foco e sugestões** | Modo Foco (esconde apps escolhidos com um toque), "mais usados" e "recentes" calculados só no aparelho | — |
| 8 | **Exclusivo II — Gestos e atalhos** | gestos configuráveis (duplo toque, deslizar), notificações, atalhos de app no menu de segurar | — |
| 9 | **Widgets** | relógio próprio + widgets do sistema (AppWidgetHost) na grade | — |
| 10 | **Acabamento** | transições, paralaxe do papel de parede, primeira execução guiada, documentação final | — |

Regras de trabalho: privacidade local (nada de rede), respeitar os perfis de desempenho (Ultra Lite sem efeitos caros),
cada regra nova nasce no domínio com testes.
