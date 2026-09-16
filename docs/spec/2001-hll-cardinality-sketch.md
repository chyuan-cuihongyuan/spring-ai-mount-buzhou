# Spec 2001 — HyperLogLog 基数素描（effort #2001，R2）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3103–T3104，impl 1552）。
> 借鉴：Redis HLL / Flajolet 系——流式 distinct 计数不求全集，定容
> 寄存器 + 调和平均，素描可合并。

## Problem Statement

高基数字符串（会话 id / 工具调用名 / 租户名）的 distinct 计数，现状只有
两条路：HashSet 全存（内存随基数线性涨）或不存（无数）。跨实例聚合更
无从谈起——各实例的 distinct 集合无法低成本合并。

## Solution

`HllCardinalitySketch`（core/metrics，synchronized 小临界区）：

- 构造契约：precision b ∈ [4,16]（fail-fast），寄存器数 m = 2^b；
- `offer(value)`：FNV-1a 64 + splitmix64 终结混合的确定性散列——前 b
  位选寄存器、尾部前导零 rank 取 max（幂等）；`offeredCount()` 对账面
  （与 estimate 的差 = 重复率）；
- `estimate()`：调和平均 α_m·m²/Σ2^-Mj + 小值域线性计数修正
  （raw ≤ 2.5m 且有零寄存器时 m·ln(m/V)）；
- `merge(other)`：同精度寄存器逐位 max（并集语义——跨实例聚合）；
- `relativeErrorBound()` = 1.04/√m（构造期既定读数）。

## User Stories

1. 作为容量观测者，我 offer 十万个工具调用名，内存只占 m 个寄存器，
   distinct 估计误差 ≤ 理论界量级。
2. 作为多实例运维者，各实例素描 merge 后得全局 distinct——不传全集。
3. 作为对账者，offeredCount − estimate ≈ 重复率，无需另建计数器。

## Implementation Decisions

- 确定性 hash（无随机数——同输入同答案可回放测试）；
- 小值域走线性计数（单元素/小集合近精确——读数不吓人）；
- merge 精度不一致 fail-fast（跨精度合并语义留白不臆造）。

## Testing Decisions

- 单元素精确；幂等（千次重复 distinct 仍 1）；小集合近精确；大基数
  （20k）误差 ≤ 5%（3× 理论界宽容带）；merge 并集语义；畸形五型
  fail-fast；先例：SessionBloomFilterTest（确定性 hash + fail-fast 风格）。

## Out of Scope

- 不做稀疏表示（HLL++ 稀疏模式省内存优化归后续轮）；
- 不接具体读面（metrics 桥接归后续轮）。

## Further Notes

- 与 SessionBloomFilter（存在性粗筛）互补：布伦答「见过吗」，HLL 答
  「见过多少个不同的」。
