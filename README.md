# FlyPlumes

Fly à durée limitée avec des plumes, fly infini par permission.

## Compiler

Il faut un JDK 21 ou plus récent et Maven :

```
mvn clean package
```

Le jar est dans `target/FlyPlumes-1.0.0.jar` : copie-le dans `plugins/`.

Dans `pom.xml`, règle `paper.version` sur la version de paper-api de ton serveur
(docs.papermc.io, page « Project setup »). Si ton serveur tourne sous Java 25,
tu peux passer `maven.compiler.release` à 25.

## Commandes

| Commande | Rôle |
|---|---|
| `/fly` | Active / désactive le fly (si le joueur a du temps ou le fly infini) |
| `/flyplume give <joueur> <id> [quantité]` | Donne des plumes (`plume_10`, `plume_30`...) |
| `/flyplume addtime / removetime / settime <joueur> <durée>` | Gère le temps (`90`, `45s`, `10m`, `1h30m`) |
| `/flyplume check <joueur>` | Affiche le temps d'un joueur |
| `/flyplume reload` | Recharge config.yml et messages.yml |

## Permissions (LuckPerms)

- `flyplumes.use` : utiliser `/fly` et les plumes (donnée à tous par défaut)
- `flyplumes.unlimited` : **fly infini**, aucun temps consommé
  `/lp group vip permission set flyplumes.unlimited true`
- `flyplumes.admin` : commandes `/flyplume`

## Placeholders (PlaceholderAPI)

| Placeholder | Valeur |
|---|---|
| `%flyplume_time%` | `1h 5min 3s`, ou `infini` pour le fly infini |
| `%flyplume_seconds%` | secondes restantes, ou `infini` |
| `%flyplume_minutes%` | minutes restantes (arrondi au-dessus), ou `infini` |
| `%flyplume_unlimited%` | `true` / `false` |
| `%flyplume_active%` | `true` / `false` (fly en cours) |

Le texte « infini » et le format du temps se règlent dans `config.yml` (`time-format`).

## Texture Nexo

1. Crée tes deux items dans Nexo (par exemple `plume_fly_10` et `plume_fly_30`, base `FEATHER`).
2. Dans `config.yml` de FlyPlumes, mets leur id dans `nexo-id` :
   ```yaml
   items:
     plume_10:
       nexo-id: "plume_fly_10"
   ```
   Le nom et la description restent ceux de FlyPlumes. Si Nexo n'est pas là,
   le plugin retombe sur `material`.
3. Sans `nexo-id`, tu peux aussi utiliser `item-model: "monpack:plume_fly_10"`.

## CustomNameplates

Le plugin expose ses valeurs via PlaceholderAPI : mets simplement
`%flyplume_time%` dans la config de CustomNameplates (nameplate, bossbar, actionbar...).

## Conflit avec EssentialsX

EssentialsX possède déjà `/fly`. Pour que ce soit celui de FlyPlumes, ajoute dans
`plugins/Essentials/config.yml` :

```yaml
disabled-commands:
  - fly
```

puis redémarre le serveur. En attendant, `/flyplumes:fly` fonctionne toujours.
