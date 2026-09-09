# Spec 56 — 共享配额原子扣减（effort #16）

> wayfinder map：`.wayfinder/maps/effort-16.md`（T249–T253）。OSS 借鉴：LiteLLM Redis 限流器
> 原子扣减（单脚本原子「读—判—扣」）；本仓 `deleteIfValueMatches` 三栈 CAS 先例。

## Problem Statement

多实例部署 + 共享 state store（Redis/JDBC）时，per-session 日配额计数「读—改—写」
非原子：两实例并发为同一会话递增 turns/tool-calls/tokens，后写覆盖前写——计数少记，
配额上限被穿透（每个实例各自看到一半用量）。单实例语义的 per-JVM 锁对跨实例竞争无效。

## Solution

在 state store 层提供条件写原语 `compareAndSwap`（三实现真原子：内存 compute /
JDBC 条件单语句 / Redis 单 Lua），配额 Hook 的三处计数写改为有界 CAS 重试循环；
日翻越以「expect 旧值原串」收敛（只有一方重置成功）。极端竞争（重试耗尽）回退
last-write 覆写并暴露回退计数——宁可少记，不误拦截、不崩溃。配额拦截语义零变化。

## User Stories

1. 作为多实例部署的运维者，我希望任一实例发生的配额扣减都原子累计，所以配额上限不会被并发穿透。
2. 作为多实例部署的运维者，我希望 UTC 日翻越时并发重置只生效一次，所以新一天从 0 开始计数而不是分裂成多个版本。
3. 作为共享 Redis state store 的用户，我希望 CAS 由单 Lua 脚本原子完成，所以不存在「读到旧值又写回」的窗口。
4. 作为共享 JDBC state store 的用户，我希望 CAS 用条件 UPDATE/INSERT 单语句，所以无需方言特定 MERGE 即可移植。
5. 作为内存单实例用户，我希望 CAS 语义与共享 store 一致，所以单测与生产行为同构。
6. 作为 SDK 开发者，我希望 `SessionStateHandle` 暴露 compareAndSwap，所以 Hook 作者无需接触 store 细节即可写原子计数。
7. 作为 SDK 开发者，我希望自定义 SessionStateStore 只需覆写一个方法即可获得原子性，所以第三方存储接入成本低。
8. 作为 SDK 开发者，我希望默认 compareAndSwap 实现诚实标注非原子，所以我不误以为免费获得了跨实例保证。
9. 作为运维者，我希望 CAS 重试耗尽有回退计数暴露，所以我能监控到极端竞争的发生。
10. 作为既有用户，我希望配额拦截的事件名/文案/拦截点完全不变，所以升级零行为差异。
11. 作为既有用户，我希望本特性零新配置键，所以绑定矩阵与升级面不动。
12. 作为红队，我希望多线程并发递增最终计数精确等于递增次数，所以丢更新被测试钉死。
13. 作为红队，我希望「两个 HookEnvironment 共享同一 store」模拟双实例竞态，所以跨实例收敛被钉死。
14. 作为红队，我希望日翻越瞬间并发递增只产生一次重置，所以跨日计数不重复不清零。
15. 作为 API 治理者，我希望公共面快照不受方法级增补影响，所以本特性不触发类型级漂移防线。

## Implementation Decisions

- SPI：`SessionStateStore#compareAndSwap(sessionId, key, expectedValue, update)`，
  `expectedValue == null` 表键不存在才写；默认实现 get+比对+put（非原子，doc 钉住）。
- InMemory：会话级 map `compute` 原子（含 absent 分支）；沿用 admissionLock 准入口径。
- JDBC：expected 非空 → 条件 `UPDATE ... WHERE session_id=? AND state_key=? AND state_value=?`；
  expected 空 → `INSERT ... SELECT ... WHERE NOT EXISTS`（单语句，影响行数判定）。
- Redis：单 Lua（参数带 expectAbsent 标志）：比对当前 HGET value（absent 以 false 表达），
  匹配则 DEL+HSET 全字段+SADD 键集，返回 1。
- Handle：`SessionStateHandle#compareAndSwap(key, expected, update)` 以与 put 相同的
  StateEntry 构造（producer=hook、createdTurn、updatedAt）透传。
- Hook：`SessionQuotaHook` 增量/累计路径 = 读 raw → 解析（日不符按 0）→ CAS(raw, 新值)，
  失败重读重试，上限 16 次；耗尽回退普通 put + `ResilienceStats` 回退计数。
- 既有 per-JVM `sessionLocks` 保留（默认非原子 CAS 的单实例兜底），跨实例正确性只由
  store 覆写承诺（诚实分层）。

## Testing Decisions

- 好测试只钉外部行为：CAS 成败语义（absent/匹配/失配/stale）、并发不丢更新、
  拦截行为不变；不测内部重试次数实现细节。
- 模块：buzhou-core（SPI 默认 + InMemory + Hook 并发）、buzhou-store-jdbc（H2 条件语句）、
  buzhou-store-redis（伪 RedisSync Lua 路径）。
- 先例：`deleteIfValueMatches` 的既有三栈测试形态沿用。

## Out of Scope

- RunawayHook / TokenBudgetHook 同型计数原子化（fog 留位）。
- 配额语义变化（维度/窗口/拦截点/事件）。
- 新配置键与绑定矩阵变更。

## Further Notes

- api-surface 快照为类型级，方法级增补不入快照——入档说明写在 api-surface.md 无需变更。
