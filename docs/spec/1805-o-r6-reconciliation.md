# Spec 1805 — O 系 R6 对账轮（effort #1805，R6）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2811–T2812，impl 1406）。
> 对账口径延续 J/K 会话 SRE PRR：每 6 轮一全量核账——工件链 + 双门 +
> 并行吸收。

## Problem Statement

O 系已落 5 轮（R1 对账门 + R2–R5 四个新机制公共类型），需周期性核账：
全仓三门（覆盖门/快照门/对账门）是否全绿、api-surface 快照是否随新公共
类型补登、origin/main 并行提交是否已吸收、工件链四面是否一致。

## Solution

R6 对账轮三件事：

1. **全仓 `mvn clean verify`**：16 模块全量构建+测试+三门（JaCoCo ≥70%、
   enforcer 收敛、SpecCoverageTest 双向链接、ApiSurfaceSnapshotTest 快照、
   OSession1800LedgerAuditTest 对账）；
2. **快照补登**：R2–R5 新增四公共类型（SpillPressureStall/TurnDeadlineBudget/
   MemoryPromotionAudit/PrefixBlockHitStats）regenerate 快照 + api-surface.md
   O 系小节入档；
3. **并行吸收**：fetch + 合并 origin/main（J/K/L/M 并行会话提交）。

## User Stories

1. 作为 O 会话驾驶者，R6 后确信前 5 轮在 CI 口径下全绿、无欠账带入 Wave 2。
2. 作为仓库维护者，api-surface 快照与 md 双档同步——公共面无未登记漂移。
3. 作为并行会话，O 系合并点吸收了我的提交，无孤儿分叉。

## Implementation Decisions

- 对账轮零生产代码（K 会话纯对账轮先例）；本轮教训入档：kill 在途 verify
  会留部分编译态（test-classes 内部类缺失假红），对账轮 verify 必须 clean
  起步或续跑前先清场。

## Testing Decisions

- 对账的证据即 verify 输出（BUILD SUCCESS + 三门通过）与 ledger audit
  四断言绿。

## Out of Scope

- 不修非本系问题（发现即登记，修复归后续轮或归属会话）。

## Further Notes

- Wave 2（R7+）选题自「借鉴定源」静脉池，逐轮 grep 复核前沿。
