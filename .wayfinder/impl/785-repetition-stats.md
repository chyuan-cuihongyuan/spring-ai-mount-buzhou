# 785 — 打转检测触发聚合读面

**What to build:** RepetitionDetectorHook fires/blocks/maxRunSeen 三计数 + 嵌套 RepetitionStats + stats() + shim 双轨测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] fires/blocks/maxRunSeen 三计数（CAS 峰值）
- [x] RepetitionStats 嵌套 record + stats()
- [x] RepetitionStatsTest（observe-only fire/unstick block/相异不 fire/峰值保持）
- [x] spec 1032 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='RepetitionStatsTest'` 全绿。commit 见本轮 `feat(core)` 提交。
