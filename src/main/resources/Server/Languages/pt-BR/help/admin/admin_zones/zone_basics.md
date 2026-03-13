---
id: admin_zone_basics
---
# Conceitos Básicos de Zonas

Zonas são territórios controlados por admins com regras personalizadas que substituem a proteção normal de facção.

## Tipos de Zona

- **SafeZone** -- Sem PvP, sem construção, sem dano.
Ideal para áreas de spawn e centros de comércio.
- **WarZone** -- PvP sempre ativado, sem construção.
Ideal para arenas e áreas de batalha disputadas.

## Criando Zonas

`/f admin safezone <name>`
Cria uma SafeZone e reivindica seu chunk atual.

`/f admin warzone <name>`
Cria uma WarZone e reivindica seu chunk atual.

Após a criação, fique em chunks adicionais e use `/f admin zone claim <zone>` para expandir a zona.

## Gerenciando Chunks da Zona

`/f admin zone claim <zone>`
Adiciona o chunk atual à zona nomeada.

`/f admin zone unclaim <zone>`
Remove o chunk atual da zona nomeada.

`/f admin zone radius <zone> <radius>`
Reivindica um quadrado de chunks ao redor da sua posição.

## Excluindo Zonas

`/f admin removezone <name>`
Exclui permanentemente a zona e libera todos os seus chunks reivindicados.

>[!WARNING] Excluir uma zona libera todos os seus chunks instantaneamente. Isso não pode ser desfeito sem uma restauração de backup.

>[!INFO] Regras de zona **sempre substituem** regras de território de facção. Uma SafeZone dentro de terreno inimigo ainda é segura.
