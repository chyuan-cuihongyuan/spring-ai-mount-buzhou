# impl-100 — 新能力 perf 哨兵

**What to build:** outbox 扫描/索引查询/导出往返三哨兵（@Tag(perf)，nightly 激活）。

**Blocked by:** None

**Status:** done

- [x] PerfEffort7SentinelsTest 三哨兵（千条 outbox 批扫/万行索引过滤/500 消息导出往返）
- [x] WebhookOutboxPerfAccess（test-jar 同包桥，perf/跨模块直驱 append/due）
- [x] docs/perf/baseline.md 增补首轮实测；perf 组激活验证 3/3 绿

## Done

commit：见 git log（impl-100）。
