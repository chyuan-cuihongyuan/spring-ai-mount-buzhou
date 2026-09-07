# Spec 58 — outbox SCAN 读放大消减（effort #18）

> wayfinder map：`.wayfinder/maps/effort-18.md`（T259–T263）。OSS 借鉴：Helicone / Langfuse
> 高吞吐事件摄取面的批量 pipeline 与计数下推思想。

## Problem Statement

webhook outbox 的热路径存在读放大：每次事件入队（append）都要 `scanByPrefix` 全量读
出所有未决记录的完整值，只为得到一个计数（JDBC 传输全部行、Redis N 次 HGETALL 往返）；
投递调度每拍同样把整个未决集的值逐键读回（N 次往返）。pending 越大，每次入队与每拍
调度的 I/O 与反序列化成本线性放大——事件量上来后 outbox 自身成为瓶颈。

## Solution

store 层补 `countByPrefix` 下推（JDBC COUNT(*) 零行传输 / Redis 键集侧计数零值读 /
内存键迭代零值读；default = scanByPrefix().size() 兼容第三方实现），outbox 容量检查
切换到该面；Redis `scanByPrefix` 的逐键 HGETALL 改 pipelined 批量读（N 次往返 → 1 次
批量）。行为零变化：软容量语义、at-least-once 投递、死信与重排队逻辑全部不动。

## User Stories

1. 作为高吞吐 webhook 用户，我希望每次入队的容量检查不读任何记录值，所以入队成本与 pending 规模解耦。
2. 作为 JDBC 用户，我希望容量计数下推为 COUNT(*)，所以不再为一个数字传输全部行。
3. 作为 Redis 用户，我希望键集侧计数即可，所以容量检查零 HGETALL。
4. 作为 Redis 用户，我希望 due 扫描批量读值，所以每拍往返次数从 N 次降到 1 次批量。
5. 作为单进程内存用户，我希望容量计数只迭代键，所以零值反序列化。
6. 作为既有用户，我希望投递语义零变化，所以升级无行为差异。
7. 作为第三方 store 实现者，我希望 countByPrefix 有正确默认，所以不实现也正确（只是慢）。
8. 作为运维者，我希望 perf 哨兵钉住 2k pending 量级的扫描/计数耗时上界，所以退化会被 CI 抓住。
9. 作为红队，我希望计数与扫描在边界 prefix（空集/损坏值/单键）下正确，所以下推不引入语义分歧。

## Implementation Decisions

- `SessionStateStore#countByPrefix(sessionId, prefix)` default = `scanByPrefix(...).size()`。
- JDBC 覆写：`SELECT COUNT(*) WHERE session_id=? AND state_key LIKE ?`。
- Redis 覆写：SMEMBERS 键集过滤计数（值零读）。
- 内存覆写：会话 map 键迭代计数（值零读）。
- Redis scanByPrefix：匹配键收集后一次 pipeline 批量 HGETALL（结果按序装配，语义
  与逐键读一致；jedismock 同命令集可测）。
- `WebhookOutbox.pendingCount()` 切换 countByPrefix；其余方法不动。

## Testing Decisions

- 契约测试加 countByPrefix 用例（三栈同测：空集 0 / 命中计数 / prefix 边界不误计）。
- Redis 批量读与逐键读结果等价的对照测试。
- perf 哨兵：2k pending 下 count 与 scan 耗时上界（宽松量级上界，非精确值）。

## Out of Scope

- 键空间格式变更（due-time 入键等结构性改法）与存量迁移。
- ZSET 等新存储形态；多实例精确容量。

## Further Notes

- 软容量并发竞差（1 条级）语义保持——计数下推不承诺原子性。
