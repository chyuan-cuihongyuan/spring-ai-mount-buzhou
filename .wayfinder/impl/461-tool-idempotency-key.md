# 461 — 工具调用幂等键传播

**What to build:** `buzhou.idempotency.key`（sessionId:callId）per-call 注入 ToolContext + `idempotencyKeyOf` 静态读取器。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 常量 + 静态读取器 + dispatch 循环 per-call 拷贝加键
- [x] 3 用例绿 + core 全模块 1755/1755 零回归
- [x] spec 608 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `feat(core)` 提交。
