---
id: T1676
title: tools 五读面全矩阵组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1675
created: 2026-09-15
---

## Question

J 会话第 108 轮：全矩阵如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ToolsMatrixReadoutTest，TempDir+回环骨架）：五读面交叉后各自守恒 + 互不串账 + reset 独立。定向 `mvn -pl buzhou-tools -am test -Dtest='ToolsMatrixReadoutTest'` 绿。
