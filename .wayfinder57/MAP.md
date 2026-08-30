# Wayfinder Map — Buzhou outbox due 索引审计（effort #57，50 轮自迭代第 22 轮）

> effort #57，延续 #56（T359–T360 / impl-242）。主线：**spec 79 fog 项「fsck due
> 对账」**——due-time 索引有读路径自愈，但无一致性审计面：调度器看不见的索引丢失
> （记录无索引）会导致投递静默停摆。

## Destination

`WebhookOutboxAudit`（公开静态类，StoreFsck 同思想）：`audit(store)` 只读对账
三类失真——孤儿（索引无记录）/ 陈旧（索引 ts ≠ 记录 nextAttemptAt）/ 缺失（记录
无索引——投递停摆须修）；计数 + 各 10 条样本 + 记录/索引总数；`repair(store,
report, options)` 按项清/补（默认全 false safe-by-default）；不实例化
WebhookOutbox（构造期回填是写副作用——纯静态只读）。

## Notes

- 借鉴：StoreFsck（spec 29）形态复用；#57 插曲（jar 漂移）再次印证「一致性工具」价值。

## Decisions so far

- 陈旧修复 = 清旧键（正确 ts 新键由 update 双写在位；直铺场景清后转缺失可再修——
  一致性收敛循环）。
- 样本有界 10 条/类（repair 只处理样本——超限需多轮 audit/repair，诚实入档）。

## Not yet specified

- 健康面挂审计计数（webhook-outbox health details）；审计定时化（运维 cron）。

## Out of scope

- 沿用 #7–#56；自动修复（safe-by-default——repair 显式 opt-in）。

## Tickets

- [x] [T361 WebhookOutboxAudit 审计/修复面](tickets/T361-due-audit.md)（impl-243）
- [x] [T362 3 例红队（三类失真/修复闭环/干净只读）+ webhook 回归 + 收口](tickets/T362-due-audit-close.md)
