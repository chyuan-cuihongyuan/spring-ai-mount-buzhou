# Spec 1814 — 热点重平衡建议器（effort #1814，R15）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2829–T2830，impl 1415）。借鉴：
> K8s descheduler——不追求绝对均衡（代价大于收益），只消越容差的热点：
> 从过载节点搬迁建议（LowNodeUtilization 反向），调度器只管生不管搬。

## Problem Statement

Spill 分片/磁盘卷间的负载倾斜没有处置语义：热点分片拖慢全局 P99，冷分片
闲着——「从哪搬到哪搬多少」无人给出；绝对均衡又过度（搬完又倾斜回来
白折腾），缺「容差内即收手」的度。

## Solution

`HotspotRebalancer`（buzhou-spill，静态纯函数）：

- `NodeLoad(nodeId, load)` 节点负载（契约：id 非空白、load ≥ 0）；
- `suggest(moveQuantum, tolerance, loads)` → `RebalancePlan(moves,
  spreadBefore, spreadAfter)`：负载降序（并列 id 字典序）反复取最热/最冷，
  极差 ≤ 容差停手；单步量 = min(quantum, 极差−容差, 极差/2)（不越衡
  反转）；MAX_MOVES=10_000 保险丝；
- `improvementRatio()` 极差改善率（before=0 时 -1 哨兵）。

## User Stories

1. 作为 spill 治理者，输入三分片负载 9/9/0 → 建议单从 a（字典序）搬 c，
   确定性可回放审计。
2. 作为容量规划者，spreadBefore/After 与 improvementRatio 直接回答「这轮
   搬迁值不值」——改善率贴 0 就别搬（会倾斜回来）。
3. 作为框架宿主，节点语义（分片/卷/层）与负载口径自声明，纯建议零执行。

## Implementation Decisions

- 纯建议不执行（搬迁归宿主）；确定性（并列 id 字典序，同入参同出参）。
- fail-fast：量子 < 1 / 负容差 / 空白 id / 负负载；null 按空表。

## Testing Decisions

- 容差内停手；量子 spread/2 封顶不反转；均衡输入零建议；少于两节点/
  空表/null 哨兵；畸形四型 fail-fast；并列确定性。

## Out of Scope

- 不执行搬迁；不做迁移代价建模（带宽/中断权重归未来静脉）。

## Further Notes

- 与 SpillTieringAudit 互补：那是「该冷该热」的分层审计，这是「分片间
  摆平」的重平衡建议。
