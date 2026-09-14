# 1116 — 中断与异常上下文卫生（M 系 R15）

**What to build:** mcp shutdown 中断恢复 + DiskSpillStore 9 处异常上下文。

**Blocked by:** T2277 / T2278（同轮 shape+verify）。

**Status:** done

- [x] shutdown catch 收窄 + InterruptedException 恢复中断位 break
- [x] DiskSpillStore 9 处 message 补操作上下文（操作名+路径/uri/sessionId）
- [x] 隔离 worktree 验证：spill 180 + mcp 107 全绿
- [x] HEAD 既有 CounterAtomicitySpreadTest 失败记档（留归属会话）

## Done

验证：worktree 双模块测试绿。commit 见本轮 fix 提交。
