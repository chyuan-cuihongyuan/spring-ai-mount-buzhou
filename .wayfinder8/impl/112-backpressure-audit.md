# impl-112 — 观测背压审计

**What to build:** 满队语义钉住（阻塞背压而非丢弃）+ 文档/告警三面。

**Blocked by:** None

**Status:** done

- [x] 回归测试：容量 1 + 慢 store → emit 阻塞不丢、释放后零丢失
- [x] javadoc 显式化满队语义；runbook §7 增 queue.wait 告警
- [x] 新事件源核对（memory.compacted 直写 = RunawayCounters T66 先例；lenient 已测）
- [x] observability 31/31 绿；spec 39 §B

## Done

commit：见 git log（impl-112）。
