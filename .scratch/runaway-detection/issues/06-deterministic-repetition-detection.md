# 06 — 确定性重复检测（M2）

**What to build:** M2 确定性重复检测——**连续 N 次同工具同参数调用**即判定失控。`beforeTool` 维护指纹环缓冲（指纹 = 规范化 `(toolName, canonicalJson(arguments))` 的稳定哈希，复用 HITL 参数指纹手法）；连续 `repetition.consecutive` 次（默认 null=关，开启后如 3）同指纹 → `runaway.repetition` 事件 + `block`（或 `repetition.action=flag-only` 仅告警不阻断）；**不做语义相似度**（避免误杀合法分页翻读/轮询/批量循环）；与 dedup 闸门协同（dedup 命中短路时不重复计数/不误报）。可选第二规则「状态原地踏步」（连续 N 步工具结果指纹相同）标 `> 【推演】`，留实现期 + 自建评测（评测集须含合法分页/轮询正例）定案是否纳入。

**Blocked by:** 03 — 复用 `beforeTool` 指纹与按工具计数基建

**Status:** ready-for-agent

- [ ] e2e：预排连续 N 次同工具同参数 tool_call → 触发 `runaway.repetition`（payload toolName/fingerprint/count）+ block
- [ ] 合法分页/轮询/批量循环（参数变化）**不误杀**——正例不触发（确定性同参数规则天然不误伤变参循环，测试显式锁定）
- [ ] `repetition.action=flag-only` 时只发事件不阻断
- [ ] `repetition.consecutive=null` 时关闭（回归）
- [ ] dedup 协同：dedup 命中短路的重复调用不触发重复检测误报
- [ ] 「状态原地踏步」规则：实现期 + 自建评测定案是否纳入，纳入则带评测集（含合法循环正例）防误杀
