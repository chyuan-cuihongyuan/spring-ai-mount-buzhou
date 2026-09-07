# Spec 96 — outbox due 索引审计（effort #57）

> wayfinder map：`.wayfinder/maps/effort-57.md`（T361–T362）。spec 79 fog 项收口。

## Problem Statement

due-time 索引（spec 79）有读路径自愈，但无一致性审计：双写竞窗/手工干预导致的
索引丢失（记录无 due 索引）会让调度器永远看不到该记录——投递静默停摆，无告警
无日志。

## Solution

`WebhookOutboxAudit`（公开静态类，StoreFsck 形态）：
- `audit(store)` 只读对账三类失真：
  - **孤儿**——索引指向的记录已删（due() 读路径会自愈，审计提前发现）；
  - **陈旧**——索引 ts ≠ 记录当前 nextAttemptAt（update 迁键竞窗残留）；
  - **缺失**——记录无任何 due 索引（投递停摆，须修复）；
  - 计数 + 各 10 条样本键 + 记录/索引总数 + `clean()`。
- `repair(store, report, options)` 按项处理（默认全 false——safe-by-default）：
  孤儿/陈旧清键、缺失从记录重建索引。
- 纯静态只读：不实例化 WebhookOutbox（构造期回填是写副作用）。

## User Stories

1. 作为运维，我要投递停摆前发现索引丢失，所以事件不丢只是延迟。
2. 作为宿主，我要审计默认只读，所以体检不引入写风险。
3. 作为红队，我要三类失真可分别计数与采样，所以修复动作有据。

## Implementation Decisions

- 样本有界 10 条/类（repair 处理样本——超限多轮收敛，诚实入档）。
- 陈旧修复 = 清旧键（正确新键由 update 双写在位；缺失转出可再修——收敛循环）。

## Testing Decisions

- 直铺三类失真 → 计数/样本/总数逐项断言；
- repair 三项后复审计 clean（陈旧转缺失再修的收敛路径覆盖）；
- 干净 outbox 审计 clean 且键数不变（只读）。

## Out of Scope

- 健康面挂审计计数；定时审计；跨实例锁（审计时禁止写入由运维纪律保证）。

## Further Notes

- 与 StoreFsck（会话域）/ spill sweep（溢出域）并列的第三域一致性工具。
