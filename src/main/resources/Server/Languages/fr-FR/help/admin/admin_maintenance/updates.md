---
id: admin_updates
---
# Verification des mises a jour

HyperFactions peut verifier les nouvelles versions et gerer la dependance HyperProtect-Mixin.

## Commandes de mise a jour

| Commande | Description |
|----------|-------------|
| `/f admin update` | Verifier les mises a jour d'HyperFactions |
| `/f admin update mixin` | Verifier/telecharger HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Activer/desactiver le telechargement automatique |
| `/f admin version` | Afficher la version actuelle et les infos de build |

## Canaux de publication

| Canal | Description |
|-------|-------------|
| **Stable** | Recommande pour les serveurs de production |
| **Pre-release** | Acces anticipe aux fonctionnalites a venir |

>[!INFO] Le verificateur de mises a jour ne fait que notifier les nouvelles versions. Il n'installe **pas** automatiquement les mises a jour d'HyperFactions lui-meme.

## HyperProtect-Mixin

HyperProtect-Mixin est le mixin de protection recommande qui active les drapeaux de zone avances (explosions, propagation du feu, conservation de l'inventaire, etc.).

- `/f admin update mixin` verifie la derniere version
et la telecharge si une version plus recente est disponible
- Le telechargement automatique peut etre active ou desactive par serveur

>[!TIP] Apres le telechargement d'une nouvelle version du mixin, un redemarrage du serveur est necessaire pour que les changements prennent effet.

## Procedure de retour en arriere

Si une mise a jour cause des problemes :

1. Arretez le serveur
2. Remplacez le JAR du plugin par la version precedente
3. Demarrez le serveur
4. Verifiez le fonctionnement avec `/f admin version`

>[!WARNING] Revenir a une version anterieure peut necessiter une reinitialisation de la migration de configuration. Gardez toujours des sauvegardes avant de mettre a jour.
