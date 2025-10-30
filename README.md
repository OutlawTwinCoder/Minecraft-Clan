# OutlawClans (Paper 1.21.8) — v0.3.8

OutlawClans fournit une expérience de clans clé en main pour serveurs Paper 1.21.8 : création de territoire terraformé automatiquement, gestion de rôles personnalisables, fermes de ressources évolutives et menus inventaires complets (lectern & NPC) pour piloter toutes les interactions.

## Fonctionnalités principales
- **Création & suppression de clans** avec coût configurable (argent, item ou expérience).
- **Territoire 150×150** terraformé automatiquement avec lissage des bords ("feather") et six terrains 35×35 prêts pour vos bâtiments.
- **Terrains protégés** : protections anti-placement/casse et clôtures périodiques à segments/gaps pour délimiter les zones.
- **Menu inventaire multi-pages** pour gérer membres, rôles, terrains, schématiques, fermes et coffres de ressources.
- **Banque de clan** : dépôt de monnaie virtuelle, d’XP ou d’items, retrait sécurisé et distribution aux membres via un menu dédié.
- **Fermes de ressources** configurables : sélection du type (Mine/Ferme/Scierie…), choix du bâtiment (.schem), coffre lié recevant les gains périodiques et préférence d’output.
- **Système de rôles** flexible (Leader, Officier, Membre, Recrue par défaut) avec permissions togglables directement en jeu.

## Installation
1. Compiler le plugin (`mvn -DskipTests package`) ou déposer la release `OutlawClans-*.jar` dans `plugins/`.
2. Démarrer le serveur une première fois pour générer `config.yml`.
3. Ajuster la configuration (économie, schématiques disponibles, fermes de ressources, etc.).
4. (Optionnel) Charger vos schématiques WorldEdit dans `plugins/WorldEdit/schematics/`.

## Commandes & permissions (LuckyPerms)
| Commande | Description | Permission requise |
| --- | --- | --- |
| `/create clan <NomClan>` | Crée un nouveau clan au joueur. | `outlawclans.create` |
| `/create clan npc` | Invoque le NPC Marchand de Territoire à votre position. | `outlawclans.create` |
| `/clan menu` | Ouvre le menu principal du clan (lectern/NPC). | Membre du clan (aucune permission spécifique) |
| `/clan show territory` | Affiche la bordure du territoire du clan courant. | Membre du clan |
| `/show clan territoire` | Alias francophone de la commande précédente. | Membre du clan |
| `/clan currency money` | Bascule l’économie sur la monnaie virtuelle interne. | `outlawclans.admin` ou `outlawclan.admin` |
| `/clan currency experience` | Bascule l’économie sur l’XP (alias `/clan currency xp`). | `outlawclans.admin` ou `outlawclan.admin` |
| `/clan currency item <ITEM_NAME>` | Définit un item monétaire personnalisé (alias ancien `/clan currency <ITEM_NAME>`). | `outlawclans.admin` ou `outlawclan.admin` |
| `/createclancost <montant>` | Met à jour le coût de création de clan (config). | `outlawclans.admin` ou `outlawclan.admin` |
| `/clanterritorycost <montant>` | Met à jour le coût d’achat du territoire (config). | `outlawclans.admin` ou `outlawclan.admin` |
| `/clan terraform` | Relance manuellement la plateforme 150×150 + feather. | `outlawclans.admin` ou `outlawclan.admin` |
| `/deleteclan confirm` | Dissout le clan (leader uniquement). | Leader (aucune permission plugin) |
| `/leaveclan confirm` | Quitte le clan (membres non-leaders). | Membre du clan |

> 💡 *LuckyPerms* : ajoutez les permissions via `lp user <joueur> permission set outlawclans.create true` ou `lp group <groupe> permission set outlawclans.admin true`.

## Système de rôles & permissions
- **Leader** : propriétaire du clan, accès implicite à toutes les actions (non configurable).
- **Rôles configurables** : définis dans `config.yml > roles.defaults`. Chaque rôle possède un nom affiché (`name`) et une liste de permissions.
- **Permissions disponibles** (`ClanRolePermission`):
  - `BUILD_TERRAIN` — placer/casser des blocs dans les terrains.
  - `MANAGE_TERRAINS` — choisir le type de ferme, sélectionner les schématiques, relier le coffre et modifier la préférence d’output.
  - `ACCESS_FARM_CHEST` — ouvrir et vider les coffres de production.
  - `MANAGE_TREASURY` — retirer/donner la monnaie du clan et valider les améliorations.
  - `MANAGE_ROLES` — modifier les permissions des rôles et assigner les membres.
- **Menu Rôles** : accessible via le menu principal > Gestion des rôles. Le leader (et tout membre avec `MANAGE_ROLES`) peut :
  1. Modifier les permissions de chaque rôle (clic pour activer/désactiver).
  2. Assigner un rôle à un membre du clan.
- **Persistance** : les choix de rôles et permissions sont sauvegardés dans les données du clan.

## Banque du clan & monnaie
- **Mode économique** : choisissez `MONEY`, `EXPERIENCE` (points d’XP) ou `ITEM` (stack d’un item précis) via `/clan currency ...`.
- **Banque centrale** : accessible depuis le menu principal (icône coffre) pour consulter le solde et gérer les fonds.
  - Tous les membres peuvent déposer leur monnaie.
  - Les rôles avec `MANAGE_TREASURY` (et le leader) peuvent retirer ou distribuer la banque aux membres connectés.
- **Distribution ciblée** : sélectionnez un montant prédéfini (configurable `economy.amount_buttons`) puis un membre pour lui envoyer immédiatement monnaie/XP/items selon le mode actif.

## Territoires & terrains
- Après achat, le territoire génère automatiquement :
  - Une plateforme circulaire 150×150 (rayon configurable `territory.radius`).
  - Un centre (schématique `ClanCenter.schem` par défaut) avec lectern/NPC.
  - **Six terrains** (3×2) positionnés autour du centre (`building.spots`).
- Chaque terrain inclut :
  - Plateforme 35×35 (`building.plot_size`) nettoyée avant collage de schématique.
  - Protection anti-modification (listeners bloquent break/place hors permissions ou joueurs externes).
  - Clôture décorative si `building.fence.enabled` (segments de `segment_length` blocs alternés avec `gap_length`).
  - Menus inventaires accessibles depuis le centre pour gérer chaque terrain.

## Fermes de ressources
1. Via le menu Terrain > **Bâtiment**, sélectionnez un **type de ferme** configuré.
2. Choisissez une schématique autorisée pour ce type (`resource_farms.types.<id>.schematics`).
3. À la validation, le plugin :
   - Terraforme/nettoie le terrain.
   - Colle la schématique correspondante.
   - Donne au joueur un **coffre de récolte** lié (item nommé & lore configurable).
4. Le joueur place ce coffre n’importe où dans le territoire :
   - Le coffre est associé au clan + terrain + type (données PDC).
   - Seuls les membres autorisés (`ACCESS_FARM_CHEST`) peuvent l’ouvrir.
5. Toutes les `resource_farms.payout_interval_ticks` (par défaut 18 000 ticks = 15 min) :
   - Les ressources configurées pour le type sont ajoutées dans le coffre selon la **préférence d’output** choisie dans le menu Terrain.

## Configuration rapide (`config.yml`)
- `territory.*` : monde cible, rayon de base et coût.
- `building.*` : nombre de terrains, tailles, offsets (`paste_y_offset`), configuration des clôtures, schématique du centre.
- `roles.*` : rôle par défaut attribué aux nouveaux membres et rôles prédéfinis (modifiable en jeu).
- `economy.*` : mode MONEY / EXPERIENCE / ITEM, type d’item et coûts (création/territoire).
- `schematics.whitelist` : liste des `.schem` autorisées dans les menus.
- `resource_farms.*` : intervalle de distribution, nom/lore du coffre, types disponibles, outputs et schématiques associées.
- `terraform.*` & `feather.*` : paramètres de nettoyage/terraforming pour territoire et lissage des bords.

## Dépendances recommandées
- **WorldEdit** pour le chargement et le collage des schématiques.
- **LuckyPerms** (ou tout gestionnaire de permissions) pour attribuer `outlawclans.create`, `outlawclans.admin`, etc.

## Support & contributions
Pour signaler un bug ou proposer une fonctionnalité, ouvrez une issue/PR en détaillant le comportement attendu, le contexte serveur et la configuration utilisée.
