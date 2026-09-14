# 1606 · 虚拟线程 pinning 审计 + 金丝雀热路径修复（Netty 铁律思想）

> 来源：N 会话 R7（effort #1606 / T2363–T2364 / impl 1159）。借鉴对象：Netty
> 「never block the event loop」铁律 + JDK 21 虚拟线程 pinning 语义——monitor 内
> 阻塞 IO 会钉住载体平台线程，弹性尽失。

## Problem Statement

JDK 21 虚拟线程进入 synchronized 方法/块时被钉在载体线程上；若块内再做阻塞 IO
（工具远程调用、磁盘写、建连、DDL），载体线程被占满整个 IO 时长。全仓审计发现
17 组风险，其中唯一处于「每次工具调用」热路径的是 CanaryToolCallback.route——
完整工具执行包在 synchronized 里：任意工具（HTTP/MCP 远程为常态）每次调用钉住
载体线程一个工具时长，还把并行工具调用无谓串行化。

## Solution

审计入档（本 spec）+ Top1 修复：`CanaryToolCallback.route` 三段式——路由决策
（锁内，内存级）→ 工具执行（锁外）→ 计数与回滚评估（锁内）。计数原子性与回滚
最终一致不变；唯一弱化是回滚判定可能晚一次调用触发（并发交错下），事实驱动评估
语义不受影响。

## 审计发现（2026-09-15，只读扫描 buzhou-*/src/main/java）

**高危（锁内真实网络/磁盘 IO）**：
1. CanaryToolCallback.route——锁内完整工具执行（**本轮已修**）；
2. RollingJsonlWriter.appendLine/close——事件级磁盘写+每行 flush+轮转 gzip 在 monitor 内；
3. DiskSpillStore.store/usage——MB 级写盘 + Files.walk 全树在 monitor 内；
4. DefaultMcpClientRegistry.refresh/rebuildEntry——锁内建连+listToolNames RPC（低频）；
5. Redis 三后端 commands()——锁内 Lettuce connect()（每进程一次）；
6. JdbcSkillStore/JdbcToolSetSpecStore.ensureSchema——锁内 DDL（每进程一次）。

**中危（锁内 store/接口调用，生产接 JDBC/Redis 即成 IO）**：WebhookOutbox 四方法
（虚拟线程 dispatcher 放大）、TokenBudgetHook/SessionQuotaHook/PeriodBudgetHook/
EpisodeLedger（锁内 store CAS/put）、SessionArchiver、PolicyRefresher、SigningKeyRing、
EvidenceRefLedger、ToolHealthProber（默认平台线程调度——现状无险）、
PromptPrefixCache.getOrLoad（API 形态 footgun，当前调用方无 IO）。

**低危**：其余约 85 类锁内仅内存操作（窗口可忽略）。正面样例：CachedEmbeddingProvider/
TtlCachingToolCallback（IO 在锁外）、HarnessToolCallingManager（已注 synchronized→
ReentrantLock 迁移说明）、SessionDrainCoordinator（latch.await 在 monitor 外）。

**后续修复排队**（每项独立小轮）：RollingJsonlWriter/DiskSpillStore（专用写线程+
有界队列，或 ReentrantLock 降 pinning）；store-in-lock 家族（锁内只做内存判定，
CAS 交 store 原子性）；建连/DDL（锁外完成后进锁提交标记）。

## User Stories

1. 作为运维者，我想让工具执行不钉住载体线程，所以并行工具 fan-out 在虚拟线程上的弹性是真实的。
2. 作为开发者，我想让审计结论可追溯，所以 17 组风险与修复排队入档 spec。
3. 作为开发者，我想并发语义有测试钉住，所以双 latch 并行断言固化「执行在锁外」。

## Testing Decisions

- `CanaryConcurrencyTest`：双 latch——两臂工具各自进入阻塞段后互放行；若执行仍在
  monitor 内（串行化）第二臂永远进不来 → await 超时断言失败。附带计数守恒
  （各臂恰一次）与 view 读数。
- 回归：`CanaryToolCallbackTest` 7 用例零变化。

## Out of Scope

- 其余 16 组风险的本轮修复（排队见审计节——每项独立轮次）。
- JDK 24+ 的 pinning 事件监测 API（基线 JDK 21 无此 API）。

## Further Notes

- 测试用两个单臂实例（100%/0%）替代随机分流的双臂并行——确定性。
