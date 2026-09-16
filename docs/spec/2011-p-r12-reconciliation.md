# Spec 2011 — P 会话 R12 对账轮（effort #2011，R12）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3123–T3124，impl 1562）。
> R6k 对账轮第二例（Wave 2 收口）。

## Problem Statement

Wave 2（R7–R11）新增 5 个公共类型（LastWriteWinsRegister /
FrequencySketch / RetryHostExclusion / FlagEvaluator / DecisionCache）
未入快照；R11 push 网络中断积压待推；五轮工件链需机器核账。

## Solution

R6k 对账四件事（R6 同款）：快照 1006→1011（+5 全 P 系）+
api-surface.md 五行 + CONTEXT 905→910 + 全仓 mvn verify 三门全绿 +
P 对账门核账 + push 补推（含 R11 积压）。

## User Stories

1. 作为仓库守门者，快照/文档/计数三面同步——公共面账实一致。

## Testing Decisions

- verify 全绿本身即测试；对账门常驻复跑（spec 2000–2011 十二号四件套）。

## Out of Scope

- 不动 M 会话 stash 半成品。

## Further Notes

- push 网络波动改为对账轮集中重试（内容轮不阻塞）。
