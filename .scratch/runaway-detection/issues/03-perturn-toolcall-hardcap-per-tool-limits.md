# 03 — 轮次级工具调用硬顶 + 按工具单独限额

**What to build:** 在 `beforeTool` 切面递增本轮工具调用数并校验硬顶；按工具名（通配匹配）单独限额。`beforeTool` 递增「本轮工具调用数」（每次实际执行的工具调用），`beforeTurn` 重置；超 `per-turn.max-tool-calls` 返回 `Block(reason)`（reason 回注为工具结果文本，对齐 `beforeTool` Block 语义）；按工具限额 `per-tool.<glob>.max-calls`（map，默认空=不限，复用既有 `ToolPolicyMatcher` 通配匹配：exact 优先 → 最长前缀 `*`，与 spill/guard 工具策略匹配一致），超限返回 Block 或降级「工具未执行」结果；`runaway.hard-stop` reason=tool-calls + `runaway.per-tool-exceeded` 事件。注意按工具 `<glob>` 是 yml 层工具名通配，**不是**四层 policy 的工具级覆盖（同 07 背压 M1 口径）。

**Blocked by:** 01 — 复用其 Hook 骨架与 e2e 装配

**Status:** ready-for-agent

- [ ] e2e：`per-turn.max-tool-calls=N` 时工具被调 >N 次触发 `runaway.hard-stop`（reason=tool-calls）
- [ ] e2e：`per-tool.expensive_*.max-calls=2` 时第 3 次调用触发 `runaway.per-tool-exceeded`（payload toolName/limit/value）+ Block/降级结果
- [ ] 通配匹配：exact 优先、最长前缀 `*` 兜底（与既有工具策略匹配口径一致）
- [ ] 工具调用计数每轮 `beforeTurn` 重置（不影响下一轮）
- [ ] 计数针对实际执行的工具调用（与 dedup 短路、许可超时的关系实现期核验并注释）
- [ ] 不配置时 null/空=不限（回归）
