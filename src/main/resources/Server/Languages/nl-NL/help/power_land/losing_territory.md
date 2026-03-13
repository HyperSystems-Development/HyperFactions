---
id: power_losing
commands: overclaim
---
# Grondgebied Verliezen

Wanneer de totale power van een factie onder de kosten van de claims zakt, wordt deze raidbaar. Vijanden kunnen chunks direct onder je vandaan overclaimen.

---

## Hoe Overclaiming Werkt

`/f overclaim`

Een Officer of Leider van een vijandige factie gaat in jouw geclaimde chunk staan en voert dit commando uit. Als je factie een powertekort heeft, gaat de chunk over naar hun factie.

## De Berekening

Elke claim kost 2.0 power om te onderhouden. Als je totale power onder die drempel zakt, zijn de tekortchunks kwetsbaar.

>[!NOTE] Dit zijn standaardwaarden. Je serverbeheerder kan andere instellingen hebben geconfigureerd.

>[!WARNING] Overclaiming is permanent. Zodra een vijand een chunk overneemt, moet je het terugclaimen (of het overclaimen als zij verzwakken).

---

## Voorbeeldscenario

| Factor | Waarde |
|--------|--------|
| Leden | 5 spelers |
| Power per lid | 10 elk (start) |
| Totale power | 50 |
| Claims | 30 chunks |
| Benodigde power (30 x 2.0) | 60 |
| Tekort | 10 power te kort |

In dit voorbeeld is de factie al raidbaar vanaf het begin. Vijanden kunnen tot 5 chunks overclaimen (10 tekort / 2.0 per claim) voordat de factie evenwicht bereikt.

---

## Hoe je Overclaiming Voorkomt

- Breid niet te veel uit -- houd de totale power altijd boven je claimkosten met een buffer
- Blijf actief -- power regenereert alleen terwijl je online bent (+0.1/min)
- Vermijd onnodige sterfgevallen -- elk sterfgeval kost 1.0 power
- Werf meer leden -- meer spelers betekent meer totale power
- Unclaim ongebruikte chunks -- maak power vrij met /f unclaim

>[!TIP] Controleer je powerstatus regelmatig met /f power. Als je totale power dicht bij je claimkosten ligt, overweeg dan om minder belangrijke chunks te unclaimen voor een oorlog.
