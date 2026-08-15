# impl-90 — memory 压缩事件化

**What to build:** 微压缩实际折入时发 memory.compacted 观测事件（无折入零噪音、失败 lenient）。

**Blocked by:** None

**Status:** done

- [x] InjectionViewProcessor.setCompactionListener（BiConsumer<sessionId,Result>；空折入不通知；异常吞）
- [x] MemoryModule 装配接 ObservabilityStore 双写（compactedCount/reclaimedChars）
- [x] 测试：折叠发事件（计数/reclaimed 正）/ 小历史零事件 / 监听器故障视图不受影响——memory 89/89 绿
- [x] spec 34 §A

## Done

commit：见 git log（impl-90）。
