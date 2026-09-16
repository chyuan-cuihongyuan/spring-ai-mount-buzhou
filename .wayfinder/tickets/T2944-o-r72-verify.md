---
id: T2944
title: O 系 R72 对账轮的验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2943]
created: 2026-09-16
---

## Question]

全仓 clean verify BUILD SUCCESS 且三门全绿吗？（spec 1871 / effort #1871 / R72）

## Resolution

**mvn clean verify BUILD SUCCESS 一次过绿**（十二波连续；首轮
后台任务被会话中断打断，重跑后绿）+ 快照门五类型一致 + 对账门四断言
绿（显式退出码）。
