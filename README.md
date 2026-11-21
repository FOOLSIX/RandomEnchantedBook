# Random Enchanted Book

[English](README.md) | [简体中文](README.zh-CN.md)

Adds books that open into a random enchanted book, with enchantment pools and border colors customizable through datapacks.

## Before you start

For Minecraft **1.21.1 / NeoForge**. Right-click to open one book, or sneak-right-click to open the entire stack. With JEI installed, you can view the possible enchantments.

Install KubeJS for your Minecraft version and loader, then launch the game once to create the `kubejs` folder alongside `mods` and `config` in your instance directory. KubeJS loads `kubejs/data` as datapack content; distribute these files with your modpack.

## 1. Modify existing random enchanted books

You can customize the four existing book types independently:

| Book | Pool file | Item ID |
| --- | --- | --- |
| Common | `common.json` | `random_enchanted_book:random_enchanted_book_common` |
| Uncommon | `uncommon.json` | `random_enchanted_book:random_enchanted_book_uncommon` |
| Rare | `rare.json` | `random_enchanted_book:random_enchanted_book_rare` |
| Epic | `epic.json` | `random_enchanted_book:random_enchanted_book_epic` |

### 1. Create the pool file

Create the following file inside the `kubejs` folder:

```text
kubejs/
└── data/
    └── random_enchanted_book/
        └── random_enchanted_book/
            └── enchantment_pool/
                └── common.json
```

Full path: `kubejs/data/random_enchanted_book/random_enchanted_book/enchantment_pool/common.json`.

The repeated `random_enchanted_book` directory is required. Add the other pool files alongside `common.json` to customize those books too.

KubeJS automatically loads files in `kubejs/data` as datapack content for every world in the instance. No `pack.mcmeta` or JavaScript script is needed. Include these files in your modpack's `kubejs/data` directory when distributing it. See the [KubeJS data folder documentation](https://kubejs.com/wiki/folder-structure/data).

### 2. Choose enchantments and a color

Put this in `common.json` to give common books a green border and allow only enchantments in the vanilla namespace that have the random-loot tag and are not curses:

```json
{
  "color": "#44FF88",
  "allow": ["minecraft"],
  "required_tags": ["#minecraft:on_random_loot"],
  "excluded_tags": ["#minecraft:curse"]
}
```

Each eligible enchantment has an equal chance. Its level is then selected uniformly from that enchantment's minimum to maximum level. `color` changes only the outer border; the book artwork stays the same.

**A pool file replaces the entire original definition. Omitted fields use the defaults below, rather than inheriting the mod's settings.** For example, omitting `weights` removes the original rarity-based weight filter.

### 3. Load and try it

After saving the JSON, reopen the world or restart the server. KubeJS loads it automatically; there is no need to install a separate datapack into each save. Changes to pool JSON, including colors, require reopening the world or restarting the server; `/reload` alone does not apply them.

Get the customized book with:

```mcfunction
/give @s random_enchanted_book:random_enchanted_book_common
```

The server synchronizes the pool and color to clients. Players do not need a separate resource pack.

## 2. Add custom random enchanted books

A custom book is an item-stack variant referencing its own pool ID. It can have its own name, color and enchantment pool while using an existing random enchanted book as its base item; it does not register a new item ID.

### 1. Create an independent pool

Create `kubejs/data/modpack/random_enchanted_book/enchantment_pool/utility.json`:

```json
{
  "color": "#33AA88",
  "allow": ["minecraft"],
  "required_tags": ["#modpack:utility_books"]
}
```

Also create `kubejs/data/modpack/tags/enchantment/utility_books.json` to select the three enchantments this book can roll:

```json
{
  "replace": false,
  "values": [
    "minecraft:mending",
    "minecraft:unbreaking",
    "minecraft:efficiency"
  ]
}
```

The pool ID is **`modpack:utility`**. The path pattern is `kubejs/data/<namespace>/random_enchanted_book/enchantment_pool/<pool_name>.json`. Adding it leaves the four original book types unchanged.

### 2. Create the book and its recipe with KubeJS

Create `kubejs/server_scripts/custom_random_books.js`:

```js
ServerEvents.recipes(event => {
  const utilityBook = Item.of(
    'random_enchanted_book:random_enchanted_book_common',
    {
      'random_enchanted_book:enchantment_pool': 'modpack:utility',
      'minecraft:custom_name': Text.of('Utility Random Enchanted Book')
    }
  )

  event.shapeless(utilityBook, [
    'minecraft:book',
    'minecraft:emerald'
  ]).id('modpack:utility_book')
})
```

This recipe crafts one utility book from a book and an emerald. The essential component is `random_enchanted_book:enchantment_pool`; its value must match the new pool's ID. This component stores only the ID, with filters and the default color remaining in the pool JSON. The optional name uses a vanilla component.

### 3. Load and test

Save the files, then reopen the world or restart the server. You can also test the pool directly with:

```mcfunction
/give @s random_enchanted_book:random_enchanted_book_common[random_enchanted_book:enchantment_pool="modpack:utility"]
```

The command-created book uses the base item's name; the KubeJS recipe output uses the script's custom name. Both reference the same pool. To create more variants, add more pool JSON files and set the corresponding IDs on your recipe outputs.

The tooltip shows the pool ID, the border reads that pool's color, and JEI distinguishes variants by pool ID and shows their possible enchantments. A missing pool produces a clear error. Missing or empty pools prevent opening without consuming the book. Preserve the component when using the book in other recipe outputs or quest rewards; removing it restores the base book's default pool.

## Pool fields

All fields are optional. All specified filters must match; `deny` takes precedence over `allow`.

| Field | Default when omitted | Meaning |
| --- | --- | --- |
| `color` | `"#FFFFFF"` | Border color in `#RRGGBB` format. |
| `allow` | No restriction | Allowed enchantment namespaces, e.g. `["minecraft", "examplemod"]`. An empty array allows nothing. |
| `deny` | Exclude nothing | Excluded enchantment namespaces, e.g. `["examplemod"]`. An empty array excludes no namespaces. |
| `required_tags` | `[]` | Every listed `#tag` must be present. |
| `excluded_tags` | `[]` | Exclude enchantments with any listed `#tag`. |
| `weights` | `[]` | Accepted enchantment definition weights, from 1 to 1024. Empty accepts any weight; this does not change selection chances within the pool. |
| `min_max_level` | `1` | Lower bound on an enchantment's own maximum level. |
| `max_max_level` | `255` | Upper bound on an enchantment's own maximum level. |

The two level fields filter which enchantments qualify; they do not set the level rolled when opening a book. The lower bound must not exceed the upper bound.

The mod's built-in pools use weights **10 / 5 / 2 / 1** for common / uncommon / rare / epic, require `#minecraft:on_random_loot`, and exclude `#minecraft:curse`. Their border colors are **#FFFFFF / #F7DD30 / #3CEEF7 / #A44CE0**, respectively.

### Complete configuration example

This example uses every configurable pool field. Put it in an existing or custom pool JSON; `examplemod` is a placeholder mod ID:

```json
{
  "color": "#44FF88",
  "weights": [5, 10],
  "min_max_level": 2,
  "max_max_level": 3,
  "allow": ["minecraft", "examplemod"],
  "deny": ["examplemod"],
  "required_tags": ["#minecraft:on_random_loot"],
  "excluded_tags": ["#minecraft:curse"]
}
```

- `color` makes the border green.
- `allow` accepts the `minecraft` and `examplemod` namespaces; `deny` excludes `examplemod`, leaving only vanilla enchantments. Modded enchantments added to vanilla tags remain excluded.
- `required_tags` requires the random-loot tag; `excluded_tags` excludes curses. Tag references start with `#`.
- `weights` accepts enchantment definition weights of 5 or 10. `min_max_level` and `max_max_level` require an enchantment's own maximum level to be between 2 and 3, inclusive.

With vanilla 1.21.1 definitions and tags, the pool contains **Knockback I–II, Loyalty I–III, Quick Charge I–III and Unbreaking I–III**. The level fields filter enchantment types; they do not restrict the rolled level to II–III. All filters apply together, and multiple `required_tags` must all match.

## Use enchantment tags

To combine enchantments from several mods or tags, create your own enchantment tag. For example, add `kubejs/data/modpack/tags/enchantment/mixed_enchantments.json`:

```json
{
  "replace": false,
  "values": [
    "minecraft:unbreaking",
    "minecraft:efficiency",
    "#minecraft:treasure"
  ]
}
```

Then reference it in an existing book's pool file or your new `utility.json`:

```json
{
  "color": "#A44CE0",
  "allow": ["minecraft"],
  "required_tags": ["#modpack:mixed_enchantments"],
  "excluded_tags": ["#minecraft:curse"]
}
```

Replace or extend the tag's values with enchantment IDs from installed mods, and add their mod IDs to `allow`, or omit `allow` to accept any namespace.

`allow` and `deny` accept namespaces only, not enchantment IDs such as `minecraft:mending` or `#tag` references. To select or exclude individual enchantments, place them in a tag and reference it through `required_tags` or `excluded_tags`. Migrate old ID/tag-based filters this way.

## Notes

- Editing a built-in pool affects all books referencing it. Bind new pools to item stacks with the `random_enchanted_book:enchantment_pool` component.
- If no enchantments match, the book cannot be opened and is not consumed.
- A stack's `minecraft:dyed_color` component overrides the pool's border color, if present.
- Loot chances, rarity weights for chest drops, and enchanted-book replacement remain in `config/random_enchanted_book-common.toml`.
