---
id: T1127
title: 技能发布通道解析的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

通道化放 store 写面还是纯解析层？版本比较与回退语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 14 轮 = effort #813 / spec 813 / impl 566）：`SkillChannelResolver` 纯解析——显式通道→回退 latest→全表最高三级；点分数值段+prerelease 低于 release（semver）；同通道收敛高版本；空通道归 latest；脏条目跳过；store 零变更。
