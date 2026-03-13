---
id: admin_imports
---
# Importação de Dados

Importe dados de facção de outros plugins para migrar seu servidor para o HyperFactions.

## Comando de Importação

`/f admin import <source> [path] [flags]`

**Permissão**: `hyperfactions.admin.use`

## Fontes Suportadas

| Fonte | Descrição |
|-------|-----------|
| `elbaphfactions` | Importar dados do ElbaphFactions |
| `hyfactions` | Importar dados do HyFactions v1 |

## Flags de Importação

| Flag | Descrição |
|------|-----------|
| `--dry-run` | Validar dados sem importar nada |
| `--overwrite` | Sobrescrever facções existentes com o mesmo nome |
| `--no-zones` | Pular dados de zona durante a importação |
| `--no-power` | Pular dados de poder durante a importação |

>[!TIP] Sempre execute com `--dry-run` primeiro para pré-visualizar o que será importado e detectar problemas nos dados antes de confirmar as alterações.

## Processo de Importação

1. Um backup pré-importação é criado automaticamente
2. Mapeamentos de nomes de jogadores são carregados
3. Facções, reivindicações e zonas são convertidas
4. Os dados são validados e salvos

## Exemplos

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Usar `--overwrite` irá **substituir** qualquer facção existente que compartilhe um nome com uma facção importada. Dados de membros e reivindicações serão sobrescritos. Execute com `--dry-run` primeiro para identificar conflitos.

>[!NOTE] Alguns dados específicos da fonte (ex.: worker plots, farm plots) não têm equivalente no HyperFactions e serão registrados como avisos durante a importação.
