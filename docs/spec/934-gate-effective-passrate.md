# 934 — GateResult 有效通过率透出

> 来源：I 会话第 34 轮 = effort #934（[T1325](../../.wayfinder/tickets/T1325-gate-effective-shape.md) / [T1326](../../.wayfinder/tickets/T1326-gate-effective-verify.md) / impl 686）。spec 933 口径收口的门面透出（spec 82 EvalRunResult 9→10 参兼容构造先例同款）。

## 背景

`GateResult.passRate` 是总量口径（分母含 pruned）——剪枝 run 在 CI 门结果里看不到有效口径，「稀释了多少」需宿主自查。门结果应双口径同屏。

## 目标

- `GateResult` record 加组件 `effectivePassRate`（11 参新构造；10 参兼容构造委托、值 = NaN——旧形态语义「无剪枝概念」，调用方二进制/源兼容）；
- `EvalGate.enforce` 填充 `run.effectivePassRate()`；
- passed 判定仍用总量 passRate（CI 硬门防剪枝刷分语义不变——双口径并存不改变门行为）；
- summary() 文本附 effectivePassRate（有剪枝时）。

## 兼容性

record 加组件（10→11 参）：兼容构造保留源/二进制兼容；passed 判定语义零变化。
