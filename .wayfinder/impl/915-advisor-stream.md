# 915 — ObservabilityAdvisor 流式路径补测（R13）

**What to build:** ObservabilityAdvisorStreamTest（7 用例：happy path/usage-only/toolCalls 抑制/无 completion 跳 TPOT/流错/取消/parent 三级回退；流式 harness 从零搭建）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ObservabilityAdvisorStreamTest（流式 harness + 7 用例）
- [x] spec 1212 + README 行
- [x] 验证：7 用例全绿；ObservabilityAdvisor 56%→69%（累计口径）

## Done

验证：定向 7/7 全绿；模块全量绿；ObservabilityAdvisor 分支 69%（隔离 worktree 累计口径）。commit 见本轮 `test(observability)` 提交。
