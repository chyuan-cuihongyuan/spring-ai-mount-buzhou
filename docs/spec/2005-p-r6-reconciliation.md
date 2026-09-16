# Spec 2005 — P 会话 R6 对账轮（effort #2005，R6）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3111–T3112，impl 1556）。
> R6k 对账轮首例（O 系惯例：全仓 verify + 快照补登前置 + 台账核账 +
> push 重试）。

## Problem Statement

Wave 1（R1–R5）新增 4 个公共类型（HllCardinalitySketch /
ExponentialWindowCounter / MemoryStrengthScore / PhiAccrualFailureDetector）
未入快照——全仓 verify 的快照门必红；并行 O 会话 R73–R75 的 3 个类型
（BurstCreditAccount / ScheduleFloat / RegexRiskAudit）+1（impl 1474 系）
同样未补登。五轮工件链与 README/台账一致性需机器核账。

## Solution

R6 对账轮四件事：

- 快照补登前置：reactor 形态（-am）跑 regenerateSnapshot → 快照
  998→1006 行（+8：P 系 4 + O 系代补 4）；api-surface.md 八行同步
  （带 spec 注释）；CONTEXT.md 公共面计数 897→905 刷新；
- 全仓 `mvn verify`：16 模块三门（覆盖门 JaCoCo ≥70% / enforcer /
  快照门 + SpecCoverage + P 系对账门）全绿；
- 台账核账：PSession2000LedgerAuditTest 四面互证（spec 2000–2005 ×
  票对 × impl 1551–1556 × README 行）；
- push 重试：github 不通期间的积压提交（O 的 c0f72f26 起 + P 系六
  提交）在恢复后 fetch 双查 + 补推。

## User Stories

1. 作为仓库守门者，快照与 md 文档同步——公共面账实一致。
2. 作为并行会话（O），代补登让其 R78 对账轮直接受益（快照门已绿）。

## Implementation Decisions

- regenerate 用 `-pl buzhou-spring-boot-starter -am test
  -Dtest='ApiSurfaceSnapshotTest#regenerateSnapshot'
  -Dbuzhou.api-snapshot.regenerate=true
  -Dsurefire.failIfNoSpecifiedTests=false`（reactor classes 形态）。

## Testing Decisions

- verify 全绿本身即测试；P 对账门常驻复跑。

## Out of Scope

- 不动 M 会话 stash 半成品；不重排 O 系排程。

## Further Notes

- 对账节奏：R6k（每 6 轮）沿用；快照补登作为对账轮前置例行。
