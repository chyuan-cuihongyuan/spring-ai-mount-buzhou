# Spec 1823 — O 系 R24 对账轮（effort #1823，R24）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2847–T2848，impl 1424）。
> 对账口径延续 R6/R12/R18：快照补登前置 + 全仓 clean verify + 台账四断言。

## Problem Statement

O 系已落 23 轮，需第四次周期核账：R19–R23 五个新公共类型快照补登、三门
全绿、GitHub 二次中断期积压提交（R22/R23）待恢复补推、Wave 5 排程落图。

## Solution

R24 对账轮四件事：

1. **快照补登前置**：五类型（ReadAheadAdvisor/BudgetPacingCurve/
   RestartSpreadPlan/PriorityInversionExposure/ChaosBudgetGate）；
2. **全仓 `mvn clean verify`**（一次过绿目标）；
3. **台账核账**：ledger audit 四断言；
4. **补推确认**：GitHub 中断期积压（00078314/5193ca71）恢复即推。

## User Stories

1. 作为 O 会话驾驶者，R24 后 Wave 5 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；README 行**先落再 verify**（R18 漏行教训内化为本轮
  步骤序）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿。

## Out of Scope

- 不修非本系问题。

## Further Notes

- GitHub 网络二次中断（R22 起两轮），本地积压策略验证有效。
