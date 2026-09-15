# Spec 1832 — 覆写环形缓冲（effort #1832，R33）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2865–T2866，impl 1433）。借鉴：
> LMAX Disruptor——固定容量环形槽位满则覆写最老，永不阻塞写入方，代价
> （老样本丢失）显式入账。

## Problem Statement`

最近窗采样（延迟/事件/读面窗口）到处手搓 List 截断：写入方可能被「窗口
维护逻辑」挡在主路上，或覆写了老样本却无账可查——「不挡主路 + 丢得起
有数」的窗口基建缺位。

## Solution

`OverwritingRingBuffer`（core/concurrent，synchronized 小临界区）：

- `add(item)` 满则覆写最老（overwrites 计数——丢了多少老样本可审计）；
- `items()` 当前窗快照（最老到最新序，防御拷贝——后续 add 不影响已取）；
- `stats()` → `Snapshot(capacity, size, overwrites)` + `hasOverwritten()`；
- 契约 fail-fast：capacity < 1、null 元素。

## User Stories

1. 作为读面作者，窗口基建一行接入——「样本封顶 N」从手搓截断变环形覆写，
   写入方永不等待。
2. 作为审计者，overwrites=200 → 这个窗口丢过 200 个老样本，读数新鲜度
   心里有数。
3. 作为并发用户，synchronized 小临界区保证槽位翻转与读数原子。

## Implementation Decisions

- 环形头指针单点推进（head 即下一写位）；oldest = head − size（模长回绕）。
- 防御拷贝换隔离性（快照后写入不串读）。

## Testing Decisions

- 容量内 FIFO；满覆最老+计数（5 入 3 容 → [3,4,5] overwrites=2）；容量 1
  退化只留最新；防御拷贝；畸形两型 fail-fast。

## Out of Scope

- 不做无锁变体（CAS 优化归未来静脉）；不做阻塞等待（那是 BoundedQueue 族）。

## Further Notes

- 与 DelayedJobQueue 正交：那是延迟调度，这是最近窗采样基建。
