---
id: T1000
title: 能力门决策审计读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

502 能力门拒绝只走异常面——deny 决定无留痕无聚合，排障要翻日志。加审计读数面吗？admit 要不要逐条？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 1 轮 = effort #700 / spec 700 / impl 600）：加 `CapabilityDecisionAudit`——deny 逐条环形（64 条+dropped 计数）、admit 只计数（量级折中——逐条 admit 会刷掉 deny 留痕）、denyByModel 聚合、snapshot() 不可变报告。纯旁路读数，拒绝行为零变化。装配随 CapabilityPresentCondition 同条件；advisor 3 参构造（nullable 审计，2 参向后兼容）。OPA Decision Logs 思想。
