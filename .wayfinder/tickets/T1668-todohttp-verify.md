---
id: T1668
title: todo×http 跨工具工作流组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1667
created: 2026-09-15
---

## Question

J 会话第 104 轮：跨工具组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TodoHttpComboTest，本地回环+随机端口骨架）：交叉调用后双读面独立 + 各自计数正确。定向 `mvn -pl buzhou-tools -am test -Dtest='TodoHttpComboTest'` 绿。
