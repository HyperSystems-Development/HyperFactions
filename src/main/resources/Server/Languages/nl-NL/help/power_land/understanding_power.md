---
id: power_understanding
commands: power
---
# Power Begrijpen

Power is de kernresource die bepaalt hoeveel grondgebied je factie kan vasthouden. Elke speler heeft persoonlijke power die bijdraagt aan het factietotaal.

---

## Standaard Powerwaarden

| Instelling | Waarde |
|------------|--------|
| Maximale power per speler | 20 |
| Startpower | 10 |
| Sterfstraf | -1.0 per sterfgeval |
| Killbeloning | 0.0 |
| Regeneratiesnelheid | +0.1 per minuut (terwijl online) |
| Powerkosten per claim | 2.0 |
| Uitloggen terwijl getagd | -1.0 extra |

>[!NOTE] Dit zijn standaardwaarden. Je serverbeheerder kan andere instellingen hebben geconfigureerd.

## Hoe het Werkt

De totale power van je factie is de som van de persoonlijke power van elk lid. Je vereiste power is het aantal claims vermenigvuldigd met 2.0. Zolang de totale power boven de vereiste power blijft, is je grondgebied veilig.

>[!INFO] Power regenereert passief met 0.1 per minuut terwijl je online bent. Met die snelheid duurt het herstellen van 1.0 power ongeveer 10 minuten.

---

## Je Power Controleren

`/f power`

Toont je persoonlijke power, de totale power van je factie en hoeveel er nodig is om de huidige claims te onderhouden.

## De Gevarenzone

Als de totale power onder het vereiste bedrag voor je claims zakt, wordt je factie kwetsbaar. Vijanden kunnen je chunks overclaimen.

>[!WARNING] Meerdere sterfgevallen in korte tijd kunnen snel escaleren. Als je 5 leden hebt elk op 10 power (50 totaal) en 20 claims (40 nodig), dan brengen slechts 5 sterfgevallen in je team je naar 45 -- nog veilig. Maar 11 sterfgevallen brengt je op 39, onder de drempel van 40.

>[!TIP] Houd een powerbuffer aan. Claim niet elke chunk die je kunt betalen -- laat ruimte voor een paar sterfgevallen zonder raidbaar te worden.
