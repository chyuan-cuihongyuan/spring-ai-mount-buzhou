# Spec 1828 — 平滑加权轮询序列（effort #1828，R29）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2857–T2858，impl 1429）。借鉴：
> NGINX smooth weighted round-robin——权重比例保持且交错平滑（5:1:2 派发
> aabacaad 而非 aaaaabc 扎堆）。

## Problem Statement`

多目标（工具后端/模型池/分片）按权重派发：朴素 WRR 按权重连派（5:1:2 →
五连 a 再 b），下游负载锯齿、连接复用差；比例与平滑两全的派发序没有生成
面。

## Solution

`SmoothWeightedSequence`（core/exec，静态纯函数）：

- `sequence(weights, picks)` → picks 个下标（NGINX 平滑加权算法：每轮全员
  current += weight，取最大者派出并 current −= 总权重）；
- `counts(weights, picks)` 派发直方；
- 零权重不参与（永不派发）；确定性可回放；契约 fail-fast：负 picks、空/
  含负权重、全零权重。

## User Stories

1. 作为派发层，5:1:2 在 80 次派发恰好 50/10/20 且无三连——比例与平滑两全。
2. 作为下游，负载曲线平（无权重锯齿）、连接复用高。
3. 作为审计者，确定性序列可回放比对。

## Implementation Decisions

- 纯生成不派发（执行归宿主）；NGINX 算法逐字保留（current 累加-峰值-
  回收三步）。
- fail-fast 四型；null/负权重拒绝。

## Testing Decisions

- 比例精确（整除场景 50/10/20）；无三连同派+前 6 项含低频项；零权重排除
  +确定性；零取数空；畸形四型 fail-fast。首跑编译红（lambda 捕获循环
  变量）改布尔局部量后绿。

## Out of Scope

- 不做动态权重（运行时调权归未来静脉）；不执行派发。

## Further Notes

- 与 RouteDistributionReadout 正交：那是分布读数，这是派发序生成。
