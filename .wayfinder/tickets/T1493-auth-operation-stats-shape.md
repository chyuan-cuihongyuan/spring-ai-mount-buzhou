---
id: T1493
title: HITL 审批操作分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 22 轮：HITL 审批操作分布读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 22 轮 = effort #1021 / spec 1021 / impl 774）：缺口成立——GuardAuthApi（spec 07 HITL 步骤 4）approve/reject/revoke 全程只发**单条事件流**（guard.auth.granted/revoked 等），无进程内聚合快照：审批通过率、拒绝率、撤销量不可直读——HITL 运营水位（危险操作审批积压、拒答异常升高）需逐条翻事件。落点 buzhou-guard hook 包：实例级 approved/rejected/revoked 三 AtomicLong + 嵌套 record `AuthOperationStats` + `stats()`。与 R19 写门四分桶分轴（那轴是门判定、本轴是台账操作）。实例级；嵌套类型不动 API 快照；行为逐位不变。
