# 11 — memory · sleep-time 后台整理

**What to build:** 对账/去冗余/重排挪到 Turn 后异步整理（虚拟线程、每 session 串行），热路径零阻塞；整理全走双时序台账，可开关、失败退避、全程审计。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：SleepTimeScheduler 串行队列执行器（共享虚拟线程池、同 session 严格 FIFO）+ SleepTimeConsolidator（对最新摘要重跑对账、无摘要 NOOP、失败不外溢）+ afterTurn 频率触发钩子；配置 memory.sleep-time.enabled/every-turns；SleepTimeConsolidationTest 3 例（异步触发/串行/NOOP）；spec 01 配置表同步）

- [ ] turn 后 hook 投递 `MemoryConsolidationTask` 到专用 executor（JDK21 虚拟线程 + 每 session 串行化防写竞争）
- [ ] 整理动作：SummaryFactReconciler 对账、去冗余、P0–P3 重排、archival evidence 归档——全走双时序台账
- [ ] 触发频率可配（每 N Turn）、失败退避重试、总开关
- [ ] 整理完成/失败审计事件
- [ ] 端到端：长会话多 Turn 后响应时长不受整理影响（热路径无阻塞断言）+ 整理产物正确
- [ ] spec 01（记忆压缩）同步

> spec 12 §memory-9；[T37](../tickets/T37-memory-sleeptime-consolidation.md)。源：letta 24,230★（sleeptime_agent_frequency、共享 block 实体、后台 Run 不阻塞主响应）。
