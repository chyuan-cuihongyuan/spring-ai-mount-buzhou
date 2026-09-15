# Spec 1849 — 水库采样（effort #1849，R50）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2899–T2900，impl 1450）。借鉴：
> Knuth 水库算法 R——流长未知下均匀无放回采 k，每元素终选概率恰 k/n；
> 种子化 LCG 跨 JVM 重现（EvalOrderRotator 同口径先例）。

## Problem Statement

诊断采样（慢日志/错误样本/审计抽查）只会在「先攒全量再随机」与「取前
N（到达序偏差）」间二选一：前者内存随流量爆炸，后者采的是到达模式不是
总体——流长未知的**均匀**采样缺基建。

## Solution

`ReservoirSample`（core/observability，静态纯函数）：

- `sample(k, seed, stream)`：前 k 直接入池，第 i（1-based）个以 k/i 概率
  替换池内随机一员（终选概率 k/n 均匀、与到达序无关）；
- 种子化 Random（LCG）——同种子同样本可回放；n ≤ k 全量保序；k=0 空。

## User Stories

1. 作为诊断作者，万条慢日志流过时保 50 条均匀样本——不用先攒一万条。
2. 作为审计者，种子入档即可重放同一批样本——抽查可复核。
3. 作为框架宿主，流元素口径（traceId/事件）自声明，纯采样零状态。

## Implementation Decisions

- 纯函数（Random 调用局部）；均匀性是数学性质（算法保证），测试断言
  定容+成员性+确定性（统计均匀性不做脆断言）。

## Testing Decisions

- 短流全量保序+k=0 空+null 空；长流恰 k 且成员性；种子确定性（同种子
  同样本）；畸形两型 fail-fast。

## Out of Scope

- 不做加权采样（A-Res 归未来静脉）；不做流式有状态封装（需持续采样归
  后续接线轮）。

## Further Notes

- 与 MisraGriesSketch 同族：一个找频项，一个采代表。
