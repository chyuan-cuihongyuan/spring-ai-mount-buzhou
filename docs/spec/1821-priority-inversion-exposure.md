# Spec 1821 — 优先级反转暴露读面（effort #1821，R22）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2843–T2844，impl 1422）。借鉴：
> OS 优先级反转（Mars Pathfinder 教训）——低优持有者挡高优等待者，关键
> 路径被无关负载拖死而系统看似无故障。

## Problem Statement

关键轮次（高优）卡在批量杂活（低优）持有的资源上时，各组件自身健康、
总账却拖死——反转暴露没有读数面：多少等待发生在「持有者不如我关键」的
资源上、最坏差多大（拖死强度），无从计数。

## Solution

`PriorityInversionExposure`（core/concurrent，静态纯函数）：

- `HeldResource(resourceId, holderRank)` / `Waiter(resourceId, waiterRank)`
 （rank 同 Unix nice：**值小=关键**；id 非空白契约）；
- `analyze(held, waiters)` → `Exposure(resources, waiters, inversions,
  worstRankGap, unknownResourceWaiters)`：holderRank > waiterRank 即反转
 （gap = 差值）；引用未持有资源的等待者诚实入账（未被挡不假报）；
- `inversionRatio()` 反转率（无等待者 -1 哨兵）。

## User Stories

1. 作为并发治理者，inversions=2/worstGap=9 → 两个等待被低 9 级的持有者
   挡住——关键路径拖死有账可查。
2. 作为审计者，inversionRatio 常态化 = 资源获取缺优先级纪律，该上优先级
   继承或收窄持锁范围。
3. 作为框架宿主，rank 口径自声明，纯读面零状态。

## Implementation Decisions

- 纯读不裁决（优先级继承/天花板协议归宿主）；同资源多次在持以首见为
  基线（互斥语义下不应发生）。
- fail-fast：空白 id（两侧）；null 任一按空表。

## Testing Decisions

- 反转计数+最坏差+比率；持有者更关键/同级不反转+未知资源诚实账；空表/
  null 哨兵；畸形 fail-fast。

## Out of Scope

- 不实现优先级继承/天花板协议；不做等待时长建模（时长归 timing 族）。

## Further Notes

- 与 AgentBulkhead 正交：那是隔离舱容量，这是持有关系的反转暴露。
