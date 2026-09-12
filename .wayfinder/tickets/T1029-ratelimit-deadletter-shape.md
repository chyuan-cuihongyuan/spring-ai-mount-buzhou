---
id: T1029
title: 限速×死信路径隔离补验的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

限速 defer 与死信路径共享投递循环——状态机隔离语义需闭环。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 40 轮 = effort #740 / spec 739 / impl 542，测试域补验轮）：defer 零状态机扰动、令牌恢复后全路径可达、重放产物不绕闸——三组用例。
