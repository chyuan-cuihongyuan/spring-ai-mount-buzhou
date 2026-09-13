# 770 — 沙箱执行结果分桶读面

**What to build:** LimitedCommandSandbox executions/timeouts/outputTruncations 三计数 + 嵌套 ExecStats + stats() + 桩 delegate 定果测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数（run/TIMEOUT/OUTPUT 归因点）
- [x] ExecStats 嵌套 record + stats()
- [x] SandboxExecStatsTest（桩 delegate：正常/超时/截断/归因不覆盖/两轴正交）
- [x] spec 1017 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='SandboxExecStatsTest,LimitedCommandSandboxTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
