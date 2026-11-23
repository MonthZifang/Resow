# Resow — Minecraft 26.3 Fabric 服务端自动补种模组

玩家**左键挖掉成熟的作物**后，模组会立刻在原地种回一株同样的作物（幼苗，AGE=0），
并从该玩家背包里消耗 1 个对应种子。背包里没有种子就不会补种，绝不会凭空生成方块或物品。

## 支持的作物

| 作物 | 消耗的种子 |
| --- | --- |
| 小麦 Wheat | 小麦种子 Wheat Seeds |
| 胡萝卜 Carrots | 胡萝卜 Carrot |
| 土豆 Potatoes | 土豆 Potato |
| 甜菜根 Beetroots | 甜菜种子 Beetroot Seeds |
| 火炬花 Torchflower | 火炬花种子 Torchflower Seeds |
| 地狱疣 Nether Wart | 地狱疣 Nether Wart |

刻意**不支持**吡咯草（Pitcher Crop）：它是双格高的作物，有上下半（HALF）状态，
只补一格会造成状态错误。

## 行为细节

- 只在**服务端**生效，客户端装了也不会有特别行为（`environment: "*"` 是为了能被服务端加载）。
- 只在方块**完全成熟**时才触发；挖未成熟的作物不会补种。
- 创造模式和旁观者模式不补种、不消耗种子。
- 补种前会检查原位置是否为空气或可被替换的方块，避免覆盖水、岩浆等。
- 消耗种子用原版的 `findSlotMatchingItem` + `removeItem`，与手动种植的消耗逻辑一致。

## 命令

所有设置都是**每个玩家独立**的（按 UUID 记录，互不影响），默认状态为「开启」。

| 命令 | 作用 |
| --- | --- |
| `/replant` | 切换自己的自动补种开/关 |
| `/yueyue` | 打开「悦悦工具箱」箱子界面（见下） |
| `/yueyue 开` | 开启自己的自动补种 |
| `/yueyue 关` | 关闭自己的自动补种 |
| `/home` | 把当前脚下的位置设成**自己的**出生点（死亡后在此重生，只影响自己） |

## 悦悦工具箱

输入 `/yueyue` 会打开一个 9 格箱子当作配置面板。第 1 格放着一个**小麦种子**图标：

- 图标名字显示当前状态（绿色「开启」/ 红色「关闭」）
- **点击这个种子**即可切换自己的自动补种开/关，与 `/yueyue 开|关` 效果相同
- 其余格子是占位，点击不会改变任何东西

## 安装

1. 服务端需要 Fabric Loader（≥0.19.3）+ Fabric API（任意 26.3 版本）。
2. 把 `build/libs/crop-replant-26.3-fabric-1.0.0.jar` 放进服务端的 `mods/` 目录。
3. 重启服务端即可，无需任何配置。

> 这是独立的小型服务端模组，不依赖、也不属于 MuoniumPlayer（后者是纯客户端模组，不能装到服务端）。

## 构建

1. 把 `gradle.properties.example` 复制为 `gradle.properties`，按本机情况修改 JDK 路径
   （若已配好 `JAVA_HOME`，相关行可留空/删除）。
2. 构建：

```powershell
.\gradlew.bat build
```

产物：`build/libs/crop-replant-26.3-fabric-1.0.0.jar`

构建环境：JDK 25、Gradle 9.6.1、fabric-loom 1.17.20；26.3 官方 jar 不混淆，
源码直接写官方类名，无需 mappings。所用 API 均已在 26.3 的 `minecraft-merged.jar`
与 Fabric API jar 上用 `javap` 逐一核对。

`gradle.properties`、`build/`、`.gradle/`、`run/`（本地测试服务器数据）均已加入 `.gitignore`，不会提交。

## 源码说明

`src/main/java/com/example/cropreplant/CropReplantMod.java`

- 通过 `PlayerBlockBreakEvents.BEFORE` 判断挖掉的是否为成熟作物，并把待补种信息暂存；
  `AFTER` 事件中方块已被破坏，据此在原位置补种并消耗种子。
- 26.3 中 `Level.setBlockState` 已改名为 `setBlock(pos, state, flags, maxUpdateDepth)`，代码按 26.3 的真实签名调用。
- 26.3 中地狱疣 `NetherWartBlock` 并非 `CropBlock` 子类，单独按 `NetherWartBlock.AGE` 判断成熟度。
