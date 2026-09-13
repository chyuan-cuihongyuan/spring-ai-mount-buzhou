# 1018 — taint 信息流控制生命周期计数读面

> 来源：J 会话第 19 轮 = effort #1017（[T1487](../../.wayfinder/tickets/T1487-taint-lifecycle-stats-shape.md) / [T1488](../../.wayfinder/tickets/T1488-taint-lifecycle-stats-verify.md) / impl 771）。与 spec 1013/1014 同族：安全判定**静默显形**（FIDES 信息流控制判定分布）。

## Problem Statement

taint 双钩子（读侧打标 `TaintTrackingHook`、写门 `TaintWriteGateHook`，FIDES 最小落地）零计数：打了多少次标、TRUSTED→UNTRUSTED 首次跃迁几次、写门四路判定（写侧检查/可信放行/已批准放行/拦截）各多少——FIDES 策略调优（写侧清单是否过大、审批是否积压、taint 是否形同虚设）无据。

## 目标

- `TaintTrackingHook` 增量：实例级 `marksApplied`（每次打标）/ `firstMarks`（TRUSTED→UNTRUSTED 首次跃迁）两 AtomicLong + 嵌套 record `TaintMarkStats` + `stats()`。
- `TaintWriteGateHook` 增量：实例级 `checkedWriteCalls` / `allowedTrusted` / `allowedApproved` / `blocked` 四 AtomicLong——守恒 **checked == trusted + approved + blocked**（非写侧工具不计）；嵌套 record `GateStats` + `stats()`。
- 行为逐位不变：标记/放行/拦截/事件语义零变化（仅加计数）。

## 兼容性

纯增量读面；嵌套类型不动 API 快照；无新配置项。

## Out of Scope

- 按 sessionId/工具名分桶（基数纪律）。
- blocked 升级事件语义变化（guard.taint.blocked 事件已有——本轮只补计数维度）。
