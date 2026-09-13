---
id: T1166
title: 技能加载延迟读数验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1165]
created: 2026-09-13
---

## Question

分位环账/溢出桶/排行如何精确证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 33 轮 = effort #832）：SkillLoadLatencyTest 5 例——环账 84/99/100 精确/挤老 max 不丢/溢出桶入账原技能 null/slowest P95 降序/脏入参。首跑 loads 环容量语义账误修正。
