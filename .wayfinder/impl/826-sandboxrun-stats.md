# 826 — 沙箱版 run_command 执行分布读面

**What to build:** SandboxRunCommandTool 静态七计数（calls/runs + blank/blacklist/workdir/timeoutParam/failures 五拒绝桶）+ 嵌套 SandboxRunStats + stats()/resetForTest() + 六路径/守恒/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 七计数落点（入口/送达/五拒绝点）
- [x] SandboxRunStats 嵌套 record + stats() + resetForTest()
- [x] SandboxRunStatsTest（送达/五拒绝/守恒/reset 等测）
- [x] spec 1074 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-tools -am test -Dtest='SandboxRunStatsTest'` 全绿 + 既有 SandboxRunCommandTool 回归绿。commit 见本轮 `feat(tools)` 提交。
