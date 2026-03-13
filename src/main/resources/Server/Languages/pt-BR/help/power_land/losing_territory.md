---
id: power_losing
commands: overclaim
---
# Perdendo Território

Quando o poder total de uma facção cai abaixo do custo das suas reivindicações, ela se torna vulnerável. Inimigos podem tomar chunks diretamente de você.

---

## Como Funciona a Tomada de Território

`/f overclaim`

Um Oficial ou Líder de uma facção inimiga fica no seu chunk reivindicado e executa este comando. Se sua facção estiver em déficit de poder, o chunk é transferido para a facção deles.

## A Matemática

Cada reivindicação custa 2.0 de poder para manter. Se o seu poder total cair abaixo desse limite, os chunks em déficit ficam vulneráveis.

>[!NOTE] Estes são valores padrão. O administrador do seu servidor pode ter configurado valores diferentes.

>[!WARNING] A tomada de território é permanente. Uma vez que um inimigo toma um chunk, você precisa reivindicá-lo novamente (ou tomá-lo de volta se eles enfraquecerem).

---

## Cenário de Exemplo

| Fator | Valor |
|-------|-------|
| Membros | 5 jogadores |
| Poder por membro | 10 cada (inicial) |
| Poder total | 50 |
| Reivindicações | 30 chunks |
| Poder necessário (30 x 2.0) | 60 |
| Déficit | 10 de poder faltando |

Neste exemplo, a facção já está vulnerável desde o início. Inimigos poderiam tomar até 5 chunks (10 de déficit / 2.0 por reivindicação) antes que a facção atinja o equilíbrio.

---

## Como Prevenir Tomadas de Território

- Não expanda demais -- sempre mantenha o poder total acima do custo das reivindicações com uma margem
- Fique ativo -- poder só regenera enquanto online (+0.1/min)
- Evite mortes desnecessárias -- cada morte custa 1.0 de poder
- Recrute mais membros -- mais jogadores significa mais poder total
- Libere chunks não utilizados -- libere poder com /f unclaim

>[!TIP] Verifique seu status de poder regularmente com /f power. Se seu poder total estiver próximo do custo das reivindicações, considere liberar chunks menos importantes antes de uma guerra.
