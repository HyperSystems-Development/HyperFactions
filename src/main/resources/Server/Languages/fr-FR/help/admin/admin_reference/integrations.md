---
id: admin_integrations
---
# Integrations de plugins

HyperFactions s'integre avec plusieurs plugins externes via des dependances optionnelles. Toutes les integrations sont facultatives et echouent gracieusement si elles ne sont pas disponibles.

## Verifier le statut des integrations

`/f admin version`
Affiche la version actuelle et les integrations detectees.

`/f admin integration`
Ouvre le panneau de gestion des integrations avec le statut detaille de chaque plugin detecte.

## Tableau des integrations

| Plugin | Type | Description |
|--------|------|-------------|
| **HyperPerms** | Permissions | Systeme de permissions complet avec groupes, heritage et contexte |
| **LuckPerms** | Permissions | Fournisseur de permissions alternatif |
| **VaultUnlocked** | Permissions/Economie | Pont de permissions et d'economie |
| **HyperProtect-Mixin** | Protection | Active les drapeaux de zone avances (explosions, feu, conservation de l'inventaire) |
| **OrbisGuard-Mixins** | Protection | Mixin alternatif pour l'application des drapeaux de zone |
| **PlaceholderAPI** | Espaces reservees | 49 espaces reservees de faction pour d'autres plugins |
| **WiFlow PlaceholderAPI** | Espaces reservees | Fournisseur d'espaces reservees alternatif |
| **GravestonePlugin** | Mort | Controle d'acces aux pierres tombales dans les zones |
| **HyperEssentials** | Fonctionnalites | Drapeaux de zone pour les foyers, points de passage et kits |
| **KyuubiSoft Core** | Framework | Integration de la bibliotheque de base |
| **Sentry** | Surveillance | Suivi des erreurs et diagnostics |

## Priorite des fournisseurs de permissions

1. **VaultUnlocked** (priorite la plus elevee)
2. **HyperPerms**
3. **LuckPerms**
4. **Repli OP** (si aucun fournisseur trouve)

>[!INFO] Les integrations sont detectees une seule fois au demarrage par reflexion. Les resultats sont mis en cache pour la session. Un redemarrage du serveur est necessaire apres l'ajout ou la suppression d'un plugin integre.

>[!TIP] Utilisez `/f admin debug toggle integration` pour activer la journalisation detaillee des integrations pour le depannage.

>[!NOTE] HyperProtect-Mixin est le mixin de protection **recommande**. Sans lui, 15 drapeaux de zone n'auront aucun effet.
