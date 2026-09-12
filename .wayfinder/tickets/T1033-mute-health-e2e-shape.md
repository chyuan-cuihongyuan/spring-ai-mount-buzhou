---
id: T1033
title: 静默标记×健康段联动补验的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

mute 与 ErrorSignaturesHealth 的传导语义未显式闭环。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 42 轮 = effort #742 / spec 742 / impl 544，测试域补验轮）：健康段 details 走 top()（spec 85）——mute 自动传导排除；snapshot 原样；unmute 回归。三断言用例。
