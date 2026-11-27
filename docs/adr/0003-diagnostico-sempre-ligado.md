# ADR 0003 — Diagnóstico embutido e sempre disponível

**Decisão.** `VLog` + crash/ANR/quadros/memória ficam no app (release inclusive), gravando local, com rotação, e só saem por ação do usuário.
O vigia e a medição de quadros rodam só com a tela visível.

**Por quê.** Problemas de fluidez aparecem em aparelhos reais (Redmi A1); sem dado, adivinhamos. Ver `docs/DIAGNOSTICO.md`.

**Limite.** O log contém nomes de pacotes abertos; nunca é enviado automaticamente.
