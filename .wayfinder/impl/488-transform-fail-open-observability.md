# 488 — 变换 fail-open 可观测

**What to build:** TransformingToolCallback 增 failOpenCount()/计数/首次 WARN。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三路失败计数 + WARN 去重 + tag 有界
- [x] 2 用例绿 + core 全模块零回归
- [x] spec 635 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `feat(core)` 提交。
