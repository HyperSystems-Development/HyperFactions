---
id: admin_updates
---
# Updatecontrole

HyperFactions kan controleren op nieuwe versies en de HyperProtect-Mixin afhankelijkheid beheren.

## Updatecommando's

| Commando | Beschrijving |
|----------|-------------|
| `/f admin update` | Controleer op HyperFactions-updates |
| `/f admin update mixin` | Controleer/download HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Schakel automatisch downloaden in/uit |
| `/f admin version` | Toon huidige versie en build-info |

## Releasekanalen

| Kanaal | Beschrijving |
|--------|-------------|
| **Stable** | Aanbevolen voor productieservers |
| **Pre-release** | Vroege toegang tot aankomende functies |

>[!INFO] De updatecontrole meldt alleen nieuwe versies. Het installeert **niet** automatisch updates voor HyperFactions zelf.

## HyperProtect-Mixin

HyperProtect-Mixin is de aanbevolen beschermingsmixin die geavanceerde zonevlaggen inschakelt (explosies, brandverspreiding, inventaris behouden, enz.).

- `/f admin update mixin` controleert op de nieuwste versie
en downloadt deze als er een nieuwere versie beschikbaar is
- Automatisch downloaden kan per server worden in- of uitgeschakeld

>[!TIP] Na het downloaden van een nieuwe mixinversie is een serverherstart vereist om de wijzigingen van kracht te laten worden.

## Terugdraaiprocedure

Als een update problemen veroorzaakt:

1. Stop de server
2. Vervang de plugin-JAR door de vorige versie
3. Start de server
4. Controleer de functionaliteit met `/f admin version`

>[!WARNING] Downgraden kan een configuratiemigratiereset vereisen. Houd altijd backups bij voordat je update.
