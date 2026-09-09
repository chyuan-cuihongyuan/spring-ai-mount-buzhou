# Wayfinder Map — Buzhou outbox SCAN 读放大消减（effort #18）

> effort #18（已闭合 2026-08-29），延续 #5–#17；收口后累计 164 轮 / impl 1–203。
> 本 effort 主线：**outbox SCAN 读放大消减**——fog 毕业生：webhook outbox 的
> `pendingCount()` 每次 append 都 scanByPrefix 全量读值（Redis = 键集 SMEMBERS +
> N 次 HGETALL 往返；JDBC = 全行传输只为 .size()）；`due()` 每拍同样 N 次值往返。
> 借鉴 Helicone / Langfuse（高吞吐事件摄取面）的批量管道思想（pipeline 化往返、
> 计数下推）——本仓适配为 store 层 `countByPrefix` 下推 + Redis 批量读。

## Destination

outbox 热路径读放大消除：append 的容量检查（pendingCount）在 JDBC 下推为
COUNT(*)（零行传输）、Redis 键集侧计数（零值读）、内存键迭代（零值读）；
Redis scanByPrefix 的 per-key HGETALL 改 pipelined 批量（N 次往返 → 1 次批量）；
perf 哨兵钉 2k pending 量级下的扫描/计数耗时上界；行为零变化（软容量语义不变）。

## Notes

- 领域/测试哲学/10K★ 政策/AFK 授权：沿用 effort #6–#17 MAP Notes。
- 外部事实源：Helicone/Langfuse 摄取面的批量 pipeline + 计数下推（ClickHouse
  prewhere / 批量插入）思想；本地裁定 = 不引入新存储形态（ZSET 等），只消既有
  抽象层内的放大（键集侧计数 + 批量值读）。
- 本地勘察（2026-08-29）：`WebhookOutbox.pendingCount()` 在 `append()`（每事件）
  调用；`scanByPrefix` Redis 实现 per-key `hgetall`；JDBC `LIKE` 已下推但 count
  场景仍传输全行。容量是软上限（并发竞差 1 条级，spec 24 已记）——计数下推不
  改语义。
- 过程教训沿用：新 SPI 方法走 default（既有实现二进制兼容）；perf 哨兵量级
  上界而非精确值（机器差异）。

## Decisions so far

- **countByPrefix 走 store SPI**（default = scanByPrefix().size()，正确但全量读）：
  JDBC 覆写 `SELECT COUNT(*)`；Redis 覆写键集 SMEMBERS 过滤计数（零 HGETALL）；
  内存覆写键迭代（零值读）。
- **Redis scanByPrefix 批量化**：匹配键一次 pipelined 批量 HGETALL（往返 1 次），
  语义与逐键读完全一致（jedismock/真 Redis 同一命令集）。
- **不动键空间格式**（due-time 入键等结构性改法 out-of-scope——迁移成本不成比例，
  fog 留位）。

## Not yet specified

- RunawayHook / TokenBudgetHook 计数写路径同型原子化（fog 沿用）。
- 观测 OLAP 导出 / skill 语义排序（fog 沿用）。
- outbox due-time 键序结构（量级证据后议）。

## Out of scope

- 沿用 effort #7–#17 Out of scope 全部条目。
- ZSET/独立存储形态引入；键空间格式变更与存量迁移。
- 多实例精确容量（软上限语义保持）。

## Tickets

初始 5 张（T259–T263，按轮逐张闭合）：

- [x] [T259 countByPrefix SPI + 三实现下推](../tickets/T259-count-spi.md)（impl-202；JDBC COUNT/Redis 键集/内存键迭代）
- [x] [T260 outbox.pendingCount 切换 + Redis scanByPrefix 批量读](../tickets/T260-outbox-batch.md)（impl-202；RedisSync.batchHgetAll async 流水线）
- [x] [T261 perf 哨兵（2k pending 计数/扫描上界）+ 契约/红队](../tickets/T261-outbox-perf.md)（impl-203；契约三栈 + testcontainers 哨兵 nightly）
- [x] [T262 文档面（runbook outbox 段 + api-surface 方法级登记）](../tickets/T262-outbox-docs.md)（impl-203）
- [x] [T263 里程碑 verify + 收口](../tickets/T263-effort18-closing.md)（全仓 verify 绿；累计 164 轮）
