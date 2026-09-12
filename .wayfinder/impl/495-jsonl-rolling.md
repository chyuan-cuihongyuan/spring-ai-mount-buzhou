# 495 — 追加式 JSONL 大小轮转

**What to build:** `core.fs.RollingJsonlWriter`（大小触发 + 代际 shift + 超龄删除 + 失败降级计数）+ HealthTimelineJsonl / ShadowComparisonJsonl 内部换装 + PromptUsageJsonl 4 参重载。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] RollingJsonlWriter（64MB×3 默认开；≤0 显式关）
- [x] 三类接入（既有构造兼容委托）
- [x] 轮转/代际封顶/显式关/重载生效/零回归用例
- [x] spec 642 + README 行
- [x] 全模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` + `-pl buzhou-resilience -am test` 绿。commit 见本轮 `feat(core)` 提交。
