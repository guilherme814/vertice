# ADR 0002 — Estado para baixo, intenções para cima

**Decisão.** A tela recebe estado pronto (`HomeUi`) e só envia intenções (`HomeActions`, `NavEvent`).
Regra mora no domínio como função pura (`HomeState.from`, `Nav.reduce`, `LayoutOps`, `BackupCodec`).
Quem decide qual tela está à frente é `Nav`; a Activity só despacha eventos.

**Por quê.** Previsibilidade (Home, Voltar, modo mover) e teste sem Android.

**Regra prática.** Se uma `if` decide comportamento do produto dentro de um Composable, ela está no lugar errado.
