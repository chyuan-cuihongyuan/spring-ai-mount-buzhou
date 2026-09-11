# 485 — 停机排水取消原因 E2E

**What to build:** ShutdownDrainCauseEndToEndTest（挂死模型 + shutdownGracefully → 事件 cause 断言）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] E2E 用例（hook 捕获 + 事件双键断言）
- [x] 3 连跑稳定 + core 全模块零回归
- [x] spec 632 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `test(core)` 提交。
