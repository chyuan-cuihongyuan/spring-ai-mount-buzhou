# Spec 4039 — DDSketch 相对误差分位（effort #4039，R40）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6079–T6080，impl 2140）。
> 借鉴：DDSketch（Datadog 对数桶相对误差分位草图）。

## Problem Statement

延迟分布分位监控的病：全量存储（内存爆炸）或固定绝对精度
直方图（低延迟端糊成一桶、高延迟端无限溢出）——**相对误差
有界 + 内存有界的分位草图面**缺失。

## Solution

`DdSketch`（core/metrics）：

- γ = (1+α)/(1−α)，对数桶：idx(v) = ⌈ln v / ln γ⌉——桶宽
  相对恒定（任意量级同精度）；
- `accept(v)`：v>0 正值域（论文口径），min/max **精确**
  旁路记录；分位估计 = γ^idx（相对高估 < γ = (1+α)/(1−α)，
  论文诚实口径）；
- `merge`：同 γ 草图桶计数相加（分位误差保证不变）；γ 不一致
  fail-fast；
- `quantile(q)`：rank=⌈q·n⌉ 升序扫桶，空草图 ISE（诚实）；
- fail-fast：α∉(0,1)、v≤0、非有限、q∉(0,1)。

## User Stories

1. 作为延迟监控作者，p50/p99 内存有界且任意量级同相对精度。
2. 作为多实例聚合作者，草图可合并——全局分位分布可得。

## Testing Decisions

- 1..1000 数据 α=0.01：p50≈503（+0.7%）、p99≈995、max 桶
  精确性旁路（min/max 精确返回 1/1000）；合并等价单建
 （两草图 1..500+501..1000 合并 = 1..1000 全量分位）；
  γ 不一致/空草图/越界 fail-fast。

## Out of Scope

- 不做负值域（ DDSketch positive-only 口径，负值另桶域
  留后）；不做 key 抽稀（collapsing 策略）；不做稠密/稀疏
  存储切换。

## Further Notes

- 与 CountMinSketch/FrequencySketch（频次族）、
  HllCardinalitySketch（基数族）同族不同面：分位数族。
  Wave 7 第四件。
- 里程碑：40/50。
