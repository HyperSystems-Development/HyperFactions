---
id: admin_world_settings
---
# Configurações por Mundo

HyperFactions suporta configuração por mundo para reivindicação, PvP e comportamento de proteção.

## Comandos de Mundo

| Comando | Descrição |
|---------|-----------|
| `/f admin world list` | Listar todas as sobrescritas de mundo |
| `/f admin world info <world>` | Mostrar configurações de um mundo |
| `/f admin world set <world> <key> <value>` | Definir uma configuração |
| `/f admin world reset <world>` | Resetar mundo para os padrões |

## Configurações Disponíveis

| Configuração | Tipo | Descrição |
|--------------|------|-----------|
| claiming_enabled | boolean | Permitir reivindicações de facção neste mundo |
| pvp_enabled | boolean | Permitir combate PvP neste mundo |
| power_loss | boolean | Aplicar perda de poder ao morrer |
| build_protection | boolean | Aplicar proteção de construção em reivindicações |
| explosion_protection | boolean | Proteger reivindicações de explosões |

## Whitelist / Blacklist de Mundos

Controle quais mundos permitem recursos de facção através do arquivo de configuração `worlds.json`:

- **Modo whitelist**: Apenas mundos listados permitem reivindicação
- **Modo blacklist**: Todos os mundos permitem reivindicação exceto os listados

>[!INFO] Configurações de mundo são armazenadas em `worlds.json` e sobrescrevem os padrões globais de `factions.json`.

## Exemplos

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- restaurar todos os padrões

>[!TIP] Desative reivindicação em mundos criativos ou de lobby para manter o sistema de facções focado na jogabilidade de sobrevivência.

>[!NOTE] Configurações por mundo têm prioridade sobre a configuração global, mas são sobrescritas por flags de zona dentro daquele mundo.
