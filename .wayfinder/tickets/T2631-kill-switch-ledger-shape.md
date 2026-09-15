---
id: T2631
title: 工具开关使用台账的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

KillSwitchUsageLedger 的形状怎么裁决？（spec 1715 / effort #1715 / R16）（spec 1715 验收/裁决）

## Resolution

实例面 synchronized 有界台账（默认 128 满逐最旧）：recordKill(tool,reason,at)/recordRestore(tool,at) 配对累计封禁时长+entries() 旧→新+counters[杀,放]；restore 配对同工具最近未配对杀；unpaired 不计时长但留痕——Unleash 开关审计思想。
