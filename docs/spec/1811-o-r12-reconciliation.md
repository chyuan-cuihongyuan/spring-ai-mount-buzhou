# Spec 1811 — O 系 R12 对账轮（effort #1811，R12）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2823–T2824，impl 1412）。
> 对账口径延续 R6：全量核账——工件链 + 三门 + 并行吸收；R6 教训吸收——
> 快照 regenerate 前置（verify 一次过绿）。

## Problem Statement

O 系已落 11 轮（R1 对账门 + R2–R11 十个新机制公共类型），需第二次周期
核账：三门是否全绿、快照是否随 R7–R11 五个新公共类型补登、GitHub 中断
期间积压提交是否已补推、工件链四面是否一致。

## Solution

R12 对账轮三件事：

1. **快照补登前置**：五类型（CheckpointLagReadout/ViolationEpisodeMerger/
   EvictionThresholdGate/FanoutPacingPlan/PrefetchCreditWindow）regenerate +
   api-surface.md O 系小节续登——在 verify 之前完成（R6 教训：先 verify 后
   补登要跑两遍）；
2. **全仓 `mvn clean verify`**：一次过绿目标；
3. **台账核账**：ledger audit 四断言 + push 状态确认（R9/R10 积压已在 R11
   轮补推，fe350325..b69b9864）。

## User Stories

1. 作为 O 会话驾驶者，R12 后 Wave 3（R13 起）无欠账带入。
2. 作为仓库维护者，快照与 md 双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；快照补登前置为本系对账轮标准步骤（R6 流程固化）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿。

## Out of Scope

- 不修非本系问题（发现即登记归属）。

## Further Notes

- GitHub 网络中断（3 连超时→空回复）期间本地积压 2 提交，恢复即补推——
  网络韧性处置入档。
