# Spec 1701 — 评测项轮换消序读面（effort #1701，R2）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2603–T2604，impl 1301）。借鉴：
> OpenAI Evals / HELM 的种子化评测顺序——**消序效应**：评测项顺序本身携带
> 信号（上下文污染/位置敏感），跨 run 换序才能分辨「分数变化是能力还是顺序」。

## Problem Statement

评测 runner 按固定顺序吃项：同一数据集连跑多轮，顺序效应（前项污染后项、
模型对位置敏感）与真实能力波动混在一起——「换了个顺序分数就变」无从归因。

## Solution

`EvalOrderRotator`（core/eval，静态纯函数）：

- `permutation(int n, int runIndex)` → 确定性种子化 Fisher–Yates 置换
  （`java.util.Random(seed)`——LCG 算法 JVM 规范固定，跨 JVM 重现），
  seed = `runIndex` 派生；runIndex 递增 → 每轮不同顺序。
- `shuffled(List<T> items, int runIndex)` → 按置换重排的**新列表**
  （多重集不变——只换序不改内容）；null/空/单项安全。
- 报告 record `OrderPlan(runIndex, permutation)` 供审计「这轮是什么序」。

## User Stories

1. 作为评测维护者，连续 5 个 run 各用 runIndex=0..4 的置换——分数方差里
   顺序贡献被摊平，剩下的才是能力波动。
2. 作为审计者，我拿 OrderPlan(3, [2,0,4,1,3]) 复现第 4 轮的精确顺序。
3. 作为框架宿主，我的 runner 零改动 opt-in：把 items 过一遍 shuffled 即可。

## Implementation Decisions

- 置换确定性优先：不用 ThreadLocalRandom（跨 JVM 不保证重现），
  用算法规范固定的 `java.util.Random`。
- 只提供排列不改 runner——与 EvalRunner 解耦（读面纪律）。

## Testing Decisions

- 确定性：同 runIndex 两次调用置换相同；多重集守恒（排序后相等）；
- 消序：runIndex 0..7 在 n=8 下置换两两不同（全 8! 排列空间的强断言：
  恰好扫过 8 个不同循环起点由种子派生保证——断言互异即可）；
- 边界：n=0/n=1 安全；shuffled 空表返回空；入参列表不被改动。

## Out of Scope

- 不改 EvalRunner 主流程；不做加权/分层抽样（归 experiment 域）。

## Further Notes

- 与 spec 1700（离散度）配套：换序后 MAD 仍大 → 能力真波动；MAD 收窄 →
  原方差多为顺序效应。
