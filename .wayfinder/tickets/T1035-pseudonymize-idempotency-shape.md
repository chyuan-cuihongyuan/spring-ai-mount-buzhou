---
id: T1035
title: 假名化×幂等占位符互操作补验的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

假名化替身在二次处理（readback 纵深）下的行为未闭环。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 43 轮 = effort #741 / spec 741 / impl 545，测试域补验轮）：假名化输出二次 pseudonymize/redact 的幂等行为用例（替身非合法 PII 形态 → 原样；边界语义入档）。
