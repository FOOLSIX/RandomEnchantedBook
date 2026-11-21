# Random Enchanted Book

[English](README.md) | [简体中文](README.zh-CN.md)

添加打开后可获得随机附魔书的书本，整合包作者可通过数据包自定义附魔池和边框颜色。

## 准备工作

适用于 **Minecraft 1.21.1 / NeoForge**。右键打开一本，潜行右键打开整组；安装 JEI 后可查看可能获得的附魔。

安装与 Minecraft 版本及加载器匹配的 KubeJS，启动一次游戏生成 `kubejs` 文件夹。它位于整合包实例目录，与 `mods`、`config` 同级。`kubejs/data` 会作为数据包内容自动加载，发布整合包时一同分发即可。

## 一、修改现有随机附魔书

四种现有书本可以分别配置：

| 附魔书 | 附魔池文件 | 物品 ID |
| --- | --- | --- |
| 普通 | `common.json` | `random_enchanted_book:random_enchanted_book_common` |
| 不常见 | `uncommon.json` | `random_enchanted_book:random_enchanted_book_uncommon` |
| 稀有 | `rare.json` | `random_enchanted_book:random_enchanted_book_rare` |
| 史诗 | `epic.json` | `random_enchanted_book:random_enchanted_book_epic` |

### 1. 创建附魔池文件

在 `kubejs` 文件夹中创建以下文件：

```text
kubejs/
└── data/
    └── random_enchanted_book/
        └── random_enchanted_book/
            └── enchantment_pool/
                └── common.json
```

完整路径为：`kubejs/data/random_enchanted_book/random_enchanted_book/enchantment_pool/common.json`。

路径中连续两层 `random_enchanted_book` 是必需的。要修改其他书，在 `common.json` 同一目录添加对应的附魔池文件即可。

KubeJS 会将 `kubejs/data` 中的文件作为数据包内容自动加载，对该实例的所有存档生效。无需创建 `pack.mcmeta`，也无需编写 JavaScript 脚本；发布整合包时，将这些文件一同放入整合包的 `kubejs/data` 目录即可。参见 [KubeJS 数据目录说明](https://kubejs.com/wiki/folder-structure/data)。

### 2. 设置附魔和颜色

在 `common.json` 中填写以下内容，将普通随机附魔书改为绿色边框，并只允许原版命名空间中具有随机战利品标签、且不是诅咒的附魔：

```json
{
  "color": "#44FF88",
  "allow": ["minecraft"],
  "required_tags": ["#minecraft:on_random_loot"],
  "excluded_tags": ["#minecraft:curse"]
}
```

池内每种附魔的抽取概率相同，随后在该附魔的最低至最高等级之间等概率抽取等级。`color` 只改变外围边框，书本图案保持原样。

**附魔池文件会整体覆盖原有定义；省略的字段使用下表中的默认值，不会继承模组原有设置。** 例如，不写 `weights` 就会取消原本按稀有度划分的附魔权重限制。

### 3. 加载并使用

保存 JSON 后，重新进入世界或重启服务端即可，KubeJS 会自动加载，无需给每个存档单独安装数据包。修改附魔池 JSON（包括颜色）后也需要重新进入世界或重启服务端，仅使用 `/reload` 不会应用这些修改。

使用以下命令获取自定义后的书：

```mcfunction
/give @s random_enchanted_book:random_enchanted_book_common
```

附魔池和颜色由服务端同步，玩家无需另外安装资源包。

## 二、新增自定义随机附魔书

自定义书是携带独立附魔池 ID 的物品变体，可以有自己的名称、颜色和附魔池，使用现有随机附魔书作为基础物品，不注册新的物品 ID。

### 1. 新建独立附魔池

创建 `kubejs/data/modpack/random_enchanted_book/enchantment_pool/utility.json`：

```json
{
  "color": "#33AA88",
  "allow": ["minecraft"],
  "required_tags": ["#modpack:utility_books"]
}
```

再创建 `kubejs/data/modpack/tags/enchantment/utility_books.json`，指定这本书可获得的三种附魔：

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

这个池的 ID 是 **`modpack:utility`**。路径规则为 `kubejs/data/<命名空间>/random_enchanted_book/enchantment_pool/<池名>.json`；新增它不会修改原有四种书。

### 2. 用 KubeJS 创建书本及其配方

创建 `kubejs/server_scripts/custom_random_books.js`：

```js
ServerEvents.recipes(event => {
  const utilityBook = Item.of(
    'random_enchanted_book:random_enchanted_book_common',
    {
      'random_enchanted_book:enchantment_pool': 'modpack:utility',
      'minecraft:custom_name': Text.of('实用随机附魔书')
    }
  )

  event.shapeless(utilityBook, [
    'minecraft:book',
    'minecraft:emerald'
  ]).id('modpack:utility_book')
})
```

该配方使用一本书和一颗绿宝石，制作一本实用随机附魔书。关键组件是 `random_enchanted_book:enchantment_pool`，值必须与新建附魔池的 ID 一致。该组件只保存 ID，筛选规则和默认颜色仍保存在池 JSON 中；名称使用原版组件，按需设置。

### 3. 加载并测试

保存文件后重新进入世界或重启服务端，也可使用命令直接测试：

```mcfunction
/give @s random_enchanted_book:random_enchanted_book_common[random_enchanted_book:enchantment_pool="modpack:utility"]
```

命令生成的书使用基础物品名称，KubeJS 配方输出使用脚本中设置的名称；两者引用同一个池。添加更多书时，分别创建新的池 JSON，并在配方输出中设置相应的池 ID 即可。

物品悬浮提示会显示池 ID，边框自动读取该池颜色，JEI 按池 ID 区分变体并显示对应附魔。池不存在时会显示明确错误；池不存在或没有符合条件的附魔时，开书失败且不消耗物品。其他配方或任务奖励也应保留该组件，移除组件后会使用基础书的默认池。

## 附魔池字段

所有字段均可省略；填写的筛选条件必须同时满足，`deny` 优先于 `allow`。

| 字段 | 省略时的默认值 | 含义 |
| --- | --- | --- |
| `color` | `"#FFFFFF"` | 边框颜色，格式为 `#RRGGBB`。 |
| `allow` | 不限制 | 允许的附魔命名空间数组，例如 `["minecraft", "examplemod"]`；空数组表示不允许任何附魔。 |
| `deny` | 不排除 | 排除的附魔命名空间数组，例如 `["examplemod"]`；空数组不排除任何命名空间。 |
| `required_tags` | `[]` | 必须同时具有所有列出的 `#标签`。 |
| `excluded_tags` | `[]` | 具有任意一个列出的 `#标签` 就会被排除。 |
| `weights` | `[]` | 接受的附魔自身定义权重，范围为 1–1024；空数组不限权重，不影响池内抽取概率。 |
| `min_max_level` | `1` | 附魔自身最高等级的下限。 |
| `max_max_level` | `255` | 附魔自身最高等级的上限。 |

两个等级字段用于筛选附魔种类，不是设置开书时抽到的等级范围；下限不能大于上限。

模组自带的普通／不常见／稀有／史诗附魔池分别按权重 **10 / 5 / 2 / 1** 筛选，要求 `#minecraft:on_random_loot` 并排除 `#minecraft:curse`，默认边框颜色分别为 **#FFFFFF / #F7DD30 / #3CEEF7 / #A44CE0**。

### 完整配置示例

以下示例使用全部可配置字段，可写入现有或自定义附魔池的 JSON。`examplemod` 为示例 modid：

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

- `color`：将边框设为绿色。
- `allow`：允许 `minecraft` 与 `examplemod` 命名空间；`deny` 排除 `examplemod`，因此最终只接受原版附魔，即使其他模组向原版标签追加附魔也不会进入池中。
- `required_tags`：要求具有随机战利品标签；`excluded_tags` 排除诅咒。标签引用以 `#` 开头。
- `weights`：只接受自身定义权重为 5 或 10 的附魔；`min_max_level` 和 `max_max_level` 要求附魔自身的最高等级在 2 到 3 之间，包含边界。

按原版 1.21.1 的定义和标签，最终可以开出**击退 I–II、忠诚 I–III、快速装填 I–III、耐久 I–III**。两个等级字段用于筛选附魔种类，不会将开书等级限制为 II–III。所有筛选条件同时生效，填写多个 `required_tags` 时必须全部满足。

## 使用附魔标签

需要组合多个模组的附魔或多个标签时，可以先创建自己的附魔标签。例如，添加 `kubejs/data/modpack/tags/enchantment/mixed_enchantments.json`：

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

然后在现有书的池文件或新建的 `utility.json` 中引用：

```json
{
  "color": "#A44CE0",
  "allow": ["minecraft"],
  "required_tags": ["#modpack:mixed_enchantments"],
  "excluded_tags": ["#minecraft:curse"]
}
```

可将标签中的条目替换或扩展为已安装模组的附魔 ID，并将对应 modid 加入 `allow`，或省略 `allow` 以接受任意命名空间。

`allow`、`deny` 只填写命名空间，不填写 `minecraft:mending` 之类的附魔 ID，也不填写 `#标签`。要选择或排除具体附魔，请创建包含这些附魔的标签，再通过 `required_tags` 或 `excluded_tags` 引用。旧格式的附魔 ID／标签筛选需要按此方式迁移。

## 补充说明

- 修改内置池影响使用该池的所有书；新增池需通过 `random_enchanted_book:enchantment_pool` 组件绑定到具体物品堆。
- 没有附魔符合筛选条件时，书本无法打开，也不会被消耗。
- 单本书若带有 `minecraft:dyed_color` 数据组件，会优先使用组件中的边框颜色。
- 箱子掉落概率、掉落书的稀有度权重及原有附魔书替换设置，仍由 `config/random_enchanted_book-common.toml` 控制。
