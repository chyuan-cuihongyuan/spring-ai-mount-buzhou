# Spec 1886 — 时钟偏斜校正（effort #1886，R87）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2973–T2974，impl 1487）。借鉴：
> Zipkin/Brave（17K+ 星）的 span 时钟偏斜校正——子 span 开始于父
> span 之前物理不可能，采集端时钟漂移让树形区间失真；钳位规则：
> 保持时长、平移起点、终点越界则收缩。

## Problem Statement

观测树（会话 ⊃ 轮次 ⊃ 调用）跨机器采集时各机时钟不完全同步：
子区间早于父开始、晚于父结束——树形包含关系失真，拓扑时序与
关键路径读数全部被污染，没有统一的钳位计算面。

## Solution

`ClockSkewClamp`（core/observability，静态纯函数 + 嵌套
ClampedSpan）：

- `clamp(childBegin, childEnd, parentBegin, parentEnd)`：①子开始
  早于父开始（负偏斜）→ 平移使 begin=parentBegin（时长保持）；
  ②平移后（或原本）子结束晚于父结束 → end=parentEnd（时长收缩，
  下限 0）；返回校正后区间 + 应用的偏斜量；
- `skewMillis(childBegin, parentBegin)`：偏斜读数（负 = 子早于父）。

## User Stories

1. 作为观测实现者，子 1000-1100 / 父 1050-1200 → 钳到 1050-1150
   （时长 100 保持）——树形包含恢复。
2. 作为拓扑分析者，skewMillis 读数指出采集端时钟快了 50ms——
   偏斜本身也是信号。
3. 作为双越界场景者，子 900-1050 / 父 1000-1100 → 先平移后收缩
   → 1000-1100——两级钳位确定性串联。

## Implementation Decisions

- 纯函数零状态；区间合法性（end ≥ begin）父子双查 fail-fast；
  钳位只动子不动父（父区间为基准）。

## Testing Decisions

- 负偏斜平移保持时长一例；正常内嵌零改动一例；终点越界收缩一例；
  双越界串联一例；畸形三型（子区间倒置/父区间倒置）fail-fast。

## Out of Scope

- 不做跨 span 树批量校正（归采集管线）；不做时钟同步协议。

## Further Notes

- 与 Span 状态分布读面（#712）互补：那是状态维度，这是时间维度
  一致性。
