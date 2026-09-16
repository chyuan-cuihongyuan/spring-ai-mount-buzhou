# Spec 2028 — 并发组闸（effort #2028，R29）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3157–T3158，impl 1579）。
> 借鉴：GitHub Actions concurrency group——同组互斥 + cancel-in-progress。

## Problem Statement

同类任务（同技能重建 / 同索引刷新 / 同目录再入）应串行：无闸则并发
堆积互相踩踏；简单互斥又堵死后来者（在跑者优先 vs 最新优先两难）；
迟到完成误释放继任者的竞态无人拦。

## Solution

`ConcurrencyGroupGate`（core/exec，synchronized 小临界区）：

- `tryEnter(group, taskId)` 三态：组空闲 → GRANTED（属主）；同 taskId
  重入幂等 GRANTED；占用 + cancelInProgress → **SUPERSEDED**（新者
  接管属主、旧者取消——只留最新防堆积）；占用不取代 → BUSY_REJECTED
  （在跑者优先）；
- `complete(group, taskId)` **属主栅栏**：仅现属主匹配才释放——被取
  代者的迟到 complete 拦下（fencedCompletions 计数），不误伤新属主；
- 读数：ownerOf / stats() 四计数（supersessions / busyRejections /
  completions / fencedCompletions）；
- 契约：group 非空非白、taskId 非空 fail-fast；组间独立。

## User Stories

1. 作为同组任务作者，cancelInProgress=true 只留最新——堆积变取代；
   false 在跑者优先——不打断。
2. 作为竞态审计者，fencedCompletions>0 即存在迟到完成竞态——显形可治。

## Testing Decisions

- 空闲授予属主追踪；重入幂等；占用拒（owner 不动）；取代接管 + 迟
  到完成栅栏；正常完成释放再入；空闲组 complete false；组间独立；
  畸形六型 fail-fast。

## Out of Scope

- 不做等待队列（拒即拒——排队归 WaitForReadyGate）；不接技能/索引
  装配（接线归后续轮）。

## Further Notes

- 与 WaitForReadyGate（就绪门）互补：那是依赖就绪前的姿态，这是同
  类任务间的互斥与取代。
