# 768 — 超时覆盖命中读面

**What to build:** ToolTimeoutOverrideStats record + timeoutMillisFor 命中/未命中计数（首中即胜口径不变）+ stats() + 幽灵模式检测测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] lookups/hits/misses 三计数 + hitsByPattern 分桶
- [x] stats() 快照（不可变）
- [x] ToolTimeoutOverrideStatsTest（精确/glob/首中即胜/幽灵模式/守恒/返回值回归）
- [x] spec 1015 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='ToolTimeoutOverrideStatsTest'` 全绿。commit 见本轮 `feat(core)` 提交。
