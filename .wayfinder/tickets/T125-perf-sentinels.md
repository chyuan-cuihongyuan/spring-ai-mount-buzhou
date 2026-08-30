---
Type: task
Status: closed
---
## Question

新能力 perf 哨兵（T93 增量）：outbox 到期扫描 / index list 过滤 / export toJson 三路径进 PerfBaselineTest 哨兵集（10 倍宽幅）？

## Resolution

AFK 自决：进。三哨兵 @Tag(perf)：outbox append+due 千条扫描 P95、InMemory index 万行 list 过滤、千消息 export toJson/fromJson 往返；阈值按首轮实测 10 倍宽幅落 docs/perf/baseline.md 增补。产 impl-100（并入 spec 22 性能面）。
