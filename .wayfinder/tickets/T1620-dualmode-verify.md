---
id: T1620
title: 双档 run_command 对账组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1619
created: 2026-09-15
---

## Question

J 会话第 82 轮：双档对账如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（DualModeRunContrastTest）：timeout=0 双档分桶对照（直执行拒/沙箱送达）+ 各自守恒 + reset 隔离。定向 `mvn -pl buzhou-tools -am test -Dtest='DualModeRunContrastTest'` 绿。
