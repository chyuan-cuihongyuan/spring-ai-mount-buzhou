# 1021 — HITL 审批操作分布读面

> 来源：J 会话第 22 轮 = effort #1021（[T1493](../../.wayfinder/tickets/T1493-auth-operation-stats-shape.md) / [T1494](../../.wayfinder/tickets/T1494-auth-operation-stats-verify.md) / impl 774）。与 R19 写门四分桶分轴：门判定 vs 台账操作（GitHub deployment protection approvals 的聚合视图思想）。

## Problem Statement

GuardAuthApi（spec 07 HITL 步骤 4）approve/reject/revoke 只发单条事件流（guard.auth.granted/revoked + confirmation.response），无进程内聚合：审批通过率、拒绝率、撤销量不可直读——危险操作审批的运营水位（积压、拒答异常升高、撤销频繁=授权过宽）需逐条翻事件流。

## 目标

- `GuardAuthApi` 增量（buzhou-guard hook 包，实例级）：`approved` / `rejected` / `revoked` 三 AtomicLong。
- 嵌套 record `AuthOperationStats(long approved, long rejected, long revoked)` + `stats()` 快照。
- 计数点：approve 成功写回 → approved+1；reject → rejected+1（不写授权语义不变）；revoke → revoked+1（幂等，逐次调用都计）。

## 兼容性

纯增量读面：approve/reject/revoke/isAuthorized 语义逐位不变；无新配置项。

## Out of Scope

- 按 sessionId/工具名分桶（基数纪律）。
- 授权 TTL 过期计数（过期发生在读侧 state store，无归因点）。
