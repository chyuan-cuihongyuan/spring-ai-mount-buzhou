---
id: T1675
title: tools 五读面全矩阵组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1643
created: 2026-09-15
---

## Question

J 会话第 108 轮：tools 域全矩阵的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R46 写/R47 读/R49 http/R51 黑名单/R56 SSRF 五读面同会话全交叉——R84 四读面（fs）与 R86 双守卫两两验证后**五读面全矩阵互不串账收口**缺失。纯测试轮第十五弹。

形状裁决：新增 `ToolsMatrixReadoutTest`（buzhou-tools）——五读面交叉调用后各自守恒保持 + 互不串账 + reset 独立隔离。零生产改动。
