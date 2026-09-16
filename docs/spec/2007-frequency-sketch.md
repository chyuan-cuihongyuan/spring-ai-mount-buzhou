# Spec 2007 — 频率素描（effort #2007，R8）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3115–T3116，impl 1558）。
> 借鉴：Caffeine W-TinyLFU——4bit Count-Min 计数板做缓存准入门控。

## Problem Statement

缓存准入需要「访问频率」次序：新条目频率低于驻留 victim 时拒绝准入
（防一次扫描污染）。精确计数要么 per-key Map（内存线性）要么无——
扫描污染与驻留污染无门控依据。

## Solution

`FrequencySketch`（core/metrics，synchronized 小临界区）：

- 4bit 槽 Count-Min 板（long[] 每词 16 槽，表长 2 的幂）；
- `increment(key)`：FNV-1a 64+splitmix64 确定性散列到相邻两槽，取
  计数较小槽 +1（防单 key 双槽独占倾斜——Caffeine 惯例）；
- `frequency(key)` = min(相邻两槽)——Count-Min 下界语义：碰撞只低估
  不高估，同 key 反复访问读数 ≈ n/2（序不变值减半——门控只需序）；
- 4bit 饱和 15 封顶不回绕（热点上限即 15）；
- 契约：expectedEntries ≥ 1、key 非 null（fail-fast）。

## User Stories

1. 作为缓存作者，newcomer.frequency < victim.frequency → 拒绝准入——
   扫描不污染驻留热点。
2. 作为容量观测者，热点排序读数可信（min 下界——低估方向安全）。

## Implementation Decisions

- 与 HllCardinalitySketch 同款确定性散列（无随机可回放）；
- 门控语义读数非精确计数（有偏半值——文档显式声明诚实边界）。

## Testing Decisions

- 重复访问频率单调（≥n/2 域）；饱和 15 不回绕；冷 key 零；双 key
  独立且保序；500 key×5 轮下界不退化（≥1）；准入次序示例（newcomer
  < victim）；畸形四型 fail-fast。

## Out of Scope

- 不做 W-TinyLFU 窗口结构（仅门控原语）；不做 reset/aging（Caffeine
  的周期性减半老化归后续轮）。

## Further Notes

- 与 HLL（基数）/指数直方图（窗口计数）三足：频率序 / distinct 数 /
  近窗数。
