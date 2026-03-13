---
id: diplomacy_enemies
commands: enemy, neutral
---
# Facções Inimigas

Declarar um inimigo é uma ação unilateral que imediatamente habilita PvP e agressão territorial contra a facção alvo. Nenhum acordo é necessário.

---

## Declarando um Inimigo

`/f enemy <faction>`

Marca instantaneamente a facção alvo como sua inimiga. Isso entra em vigor imediatamente -- nenhuma confirmação do outro lado é necessária. Requer cargo de Oficial ou superior.

## Resetando para Neutro

`/f neutral <faction>`

Encerra o status de inimigo e reseta a relação para neutro. Também requer Oficial+ e entra em vigor imediatamente.

---

## O Que o Status de Inimigo Habilita

| Efeito | Detalhes |
|--------|----------|
| PvP no território | PvP completo é habilitado no território de ambas as facções |
| Tomada de território | Você pode tomar chunks deles se estiverem em déficit de poder |
| Marcação no mapa | Território inimigo aparece em vermelho no mapa de território |
| Sem proteção | A proteção padrão de território não impede PvP inimigo |

>[!WARNING] Declarar um inimigo é uma decisão séria. Os membros deles também podem lutar com você no seu próprio território após a declaração.

---

## Considerações Estratégicas

- Declarações de inimizade são unilaterais -- você pode declarar sem o consentimento deles, mas eles também passam a te ver como hostil
- Antes de declarar, verifique o poder do alvo com /f info. Se eles forem fortes, você pode perder território em vez de ganhar
- Enfraqueça inimigos através de combate repetido para drenar o poder deles, depois tome seu terreno
- Não há limite de quantos inimigos você pode ter, mas lutar em múltiplas frentes é arriscado

>[!TIP] Use /f neutral para desescalar conflitos. Às vezes uma paz estratégica é mais valiosa do que guerra contínua.

>[!NOTE] Se você estiver aliado a uma facção e declará-la como inimiga, a aliança é rompida primeiro.
