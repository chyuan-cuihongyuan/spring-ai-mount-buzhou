---
id: T1307
title: GateResult 有效通过率透出的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 34 轮：GateResult（spec 80）透传 run 的 passRate 总量口径——spec 933 有效口径（分母排除 pruned）是否应透出到门结果？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 34 轮 = effort #934 / spec 934 / impl 686）：透出成立——剪枝 run 的 GateResult.passRate 是总量口径（分母含 pruned），宿主/CI 需同屏看到有效口径才能判断「稀释了多少」。落点 `GateResult` record 加组件 `effectivePassRate`（11 参新构造 + 10 参兼容构造委托、effectivePassRate = NaN 语义「无剪枝旧形态」——spec 82 EvalRunResult 9→10 参兼容先例同款）；`EvalGate.enforce` 填充 `run.effectivePassRate()`。passed 判定仍用总量 passRate（CI 硬门防剪枝刷分语义不变——双口径并存不改变门行为）。
