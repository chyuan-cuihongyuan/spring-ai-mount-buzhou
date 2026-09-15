# Spec 1817 — O 系 R18 对账轮（effort #1817，R18）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2835–T2836，impl 1418）。
> 对账口径延续 R6/R12：快照补登前置 + 全仓 clean verify 一次过绿 + 台账
> 四断言。

## Problem Statement

O 系已落 17 轮（三个对账门轮 + 14 个机制轮），需第三次周期核账：R13–R17
五个新公共类型快照补登、三门全绿、工件链四面一致。

## Solution

R18 对账轮三件事：

1. **快照补登前置**：五类型（HalfMessageAudit/TtlProbeStateMachine/
   HotspotRebalancer/GapBackfillPlanner/LoadShedLadder）regenerate +
   api-surface.md 续登；
2. **全仓 `mvn clean verify`**（一次过绿目标，R12 口径）；
3. **台账核账**：OSession1800LedgerAuditTest 四断言 + Wave 4（R19 起）
   排程落图。

## User Stories

1. 作为 O 会话驾驶者，R18 后 Wave 4 无欠账带入。
2. 作为仓库维护者，快照双档同步、三门全绿一次可证。

## Implementation Decisions

- 对账轮零生产代码；「快照补登前置」已固化为本系对账轮标准步骤（R6 教训
  → R12 固化 → R18 例行）。

## Testing Decisions

- 证据 = clean verify BUILD SUCCESS + ledger audit 四断言绿。

## Out of Scope

- 不修非本系问题（发现即登记归属）。

## Further Notes

- R6 期 GitHub 网络中断 3 连超时后恢复，积压补推链路已验证韧性。
