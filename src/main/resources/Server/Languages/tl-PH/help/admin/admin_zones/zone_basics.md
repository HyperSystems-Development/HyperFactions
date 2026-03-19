---
id: admin_zone_basics
---
# Mga Pangunahing Kaalaman sa Zone

Ang mga zone ay admin-controlled na teritoryo na may custom rules na nag-o-override ng normal na faction protection.

## Mga Uri ng Zone

- **SafeZone** -- Walang PvP, walang building, walang damage.
Ideal para sa mga spawn area at trading hub.
- **WarZone** -- Palaging naka-enable ang PvP, walang building.
Ideal para sa mga arena at contested battle area.

## Paggawa ng mga Zone

`/f admin safezone <name>`
Gumagawa ng SafeZone at kini-claim ang kasalukuyan mong chunk.

`/f admin warzone <name>`
Gumagawa ng WarZone at kini-claim ang kasalukuyan mong chunk.

Pagkatapos gumawa, tumayo sa mga karagdagang chunk at gamitin ang `/f admin zone claim <zone>` para palawakin ang zone.

## Pamamahala ng mga Zone Chunk

`/f admin zone claim <zone>`
Idagdag ang kasalukuyang chunk sa pinangalanang zone.

`/f admin zone unclaim <zone>`
Tanggalin ang kasalukuyang chunk mula sa pinangalanang zone.

`/f admin zone radius <zone> <radius>`
Mag-claim ng parisukat na mga chunk sa paligid ng posisyon mo.

## Pag-delete ng mga Zone

`/f admin removezone <name>`
Permanenteng dine-delete ang zone at binibitawan ang lahat ng na-claim na chunk nito.

>[!WARNING] Ang pag-delete ng zone ay agad na nagbibigyang-laya sa lahat ng chunk nito. Hindi ito pwedeng i-undo nang walang backup restore.

>[!INFO] Ang mga zone rule ay **palaging nag-o-override** ng faction territory rules. Ang SafeZone sa loob ng enemy land ay ligtas pa rin.
