# Spec 1702 — 评测集覆盖矩阵读面（effort #1702，R3）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2605–T2606，impl 1302）。借鉴：
> JaCoCo / Stryker 的覆盖矩阵思想 + scikit-learn 信息熵——「测了什么」先于
> 「测得怎样」：评测集对能力标签的分布失衡用熵显形。

## Problem Statement

评测集长期生长后没人说得清它覆盖了哪些工具/错误类别：某类工具 40 个用例、
另一类 0 个——门分数再高也是偏科高分。「标签×用例」的覆盖矩阵与均衡度缺位。

## Solution

`EvalCoverageMatrix`（core/eval，静态纯函数）：

- `build(List<Set<String>> itemLabelSets)` → `CoverageReport(itemCount,
  labelCounts（标签→用例数，字典序）, distinctLabels)`。
- `CoverageReport.missingFrom(Set<String> universe)` → 宇宙中零覆盖标签
  （字典序）——「漏测清单」。
- `CoverageReport.shannonEntropy()` → 归一化香农熵 0..1（按 ln k 归一；
  k≤1 时 0）——1=完美均衡，趋 0=偏科。测试域无新依赖（自算 ln）。

## User Stories

1. 作为评测维护者，我看到 entropy=0.42——标签分布严重偏科，优先补冷门类。
2. 作为评测维护者，missingFrom(全部 12 类) 给出 3 个零覆盖类，直接变成本轮补测题单。
3. 作为框架宿主，任何带标签的评测集零适配即可入口（Set<String> 通用）。

## Implementation Decisions

- 纯读面：不改评测 runner、不强加标签体系（宇宙由宿主声明）。
- 熵归一化用自然对数——纯 Java `Math.log`，零依赖。

## Testing Decisions

- 计数与去重：多集合并、每标签计数正确；missingFrom 差集字典序；
- 熵：单标签 0；两标签等量≈1；四标签 2:1:1:1 介于其间且单调合理；
- 边界：空集/空宇宙/全部覆盖（missing 空）。

## Out of Scope

- 不做 per-label 通过率联动（质量维度归 EvalGate）；不做加权重码。

## Further Notes

- 覆盖（本轮，测了什么）→ 离散（spec 1700，稳不稳）→ 趋势（1444，方向）
  → 门（过不过）——评测可观测四象限补齐第三象限。
