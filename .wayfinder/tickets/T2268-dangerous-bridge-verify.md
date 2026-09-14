---
id: T2268
title: 危险工具默认 HITL 自动带入桥的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2267
created: 2026-09-15
---

## Question

M 会话第 9 轮：自动带入桥如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-core,buzhou-tools,buzhou-guard -am test` 绿——
① DangerousToolRegistry 单测（register 并集/快照不可变/reset 清零）；
② guard autoconfig 桥单测：注册表有名 → 构建出的 GuardModule dangerousTools 含默认条目（requiredState=confirm_<name>）；yml 显式名不重复；开关 false 零并入；
③ tools 灌注：toolsModule 装配（write_file 开）后注册表含 write_file；
④ 既有 guard/tools 测试零回归。
