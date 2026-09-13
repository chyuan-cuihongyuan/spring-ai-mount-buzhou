# 653 — 事件丢弃按原因分类读面

**What to build:** BufferedEventDispatcher 按 reason 分类计数 + EventDropBreakdown 公共快照 + AgentSession.eventDropBreakdown() 读面（SYNC empty）；守恒不变量（ΣbyReason == dropped）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] EventDropBreakdown record（core.session，Map.copyOf 不可变 + forReason/total）
- [x] BufferedEventDispatcher：ConcurrentHashMap<String,LongAdder> 分类计数
- [x] AgentSession default + DefaultAgentSession 接线
- [x] EventDropBreakdownTest（drop-oldest 分桶 / 守恒 / SYNC empty / 快照不可变）
- [x] spec 800 + README 行 + API 快照再生

## Done

验证：`mvn -pl buzhou-core -am test` 全绿。commit 见本轮 `feat(core)` 提交。
