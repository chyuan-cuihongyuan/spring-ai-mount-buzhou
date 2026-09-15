# Spec 1857 — 保工作性审计（effort #1857，R58）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2915–T2916，impl 1458）。借鉴：
> 调度理论 work conservation（保工作性，WFQ/DRR 核心性质）——有活可干时
> 不让容量闲置；不公平可谈（权重策略），不保工作不可恕（纯浪费）。

## Problem Statement`

多队列调度（工具车道/模型池/会话分片）的公平性各有读数，但**保工作性**
无人审：一个队列积压等死、另一个队列配额在空转——容量白闲的纯浪费在
「各队列各自正常」的读数里隐形。

## Solution

`WorkConservationAudit`（core/exec，静态纯函数）：

- `Slot(queue, backlog, allocatedCapacity)` 单时隙快照契约；
- `audit(slotsPerInstant)` 逐时隙判违例（存在积压队列 且 存在「有配额
  无积压」的闲置队列）→ `Report(slots, violations, totalIdleWithBacklog,
  totalBacklogDuringViolation)`；
- `violationRatio()` 违例率 + `wasteCoverageRatio()` 浪费覆盖比（闲置
  容量/同期积压——≥1 即闲置足以清空积压，纯浪费；无违例 -1 哨兵）。

## User Stories

1. 作为调度作者，违例率 50% + 覆盖比 0.05 → 一半时隙在浪费，但闲置量
   还清不动积压（该扩容不只是重分配）。
2. 作为容量治理者，覆盖比 ≥1 → 闲置足以清积压——重分配即可救，纯浪费。
3. 作为框架宿主，队列语义自声明，纯审计不调度。

## Implementation Decisions

- 纯审计不调度（重分配归宿主）；null 内外层按空表。

## Testing Decisions

- 违例判定+两比率；健康面（全忙/闲无积压）零违例；空/null 哨兵；畸形
  三型 fail-fast。

## Out of Scope

- 不做重分配建议（HotspotRebalancer 已有）；不做权重公平（FairnessIndex
  已有）。

## Further Notes

- 与 HotspotRebalancer 互补：那是存量重平衡建议，这是调度过程的浪费审计。
