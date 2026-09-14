---
id: T1627
title: 双守卫（黑名单+SSRF）组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1625
created: 2026-09-15
---

## Question

J 会话第 86 轮：双守卫组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R51 CommandBlacklist（命令黑名单）与 R48 SsrfGuard（出网守卫）两读面在同会话协同工作——**双守卫各自计数在同一会话中的独立性与一致性**无验证。纯测试轮第四弹（R81/R82/R84 先例）。

形状裁决：新增 `DualGuardReadoutTest`（buzhou-tools）——同会话混合调用（黑名单命中命令 + SSRF 内网 HTTP）双读面互不干扰、各自守恒保持、reset 独立隔离。零生产改动。
