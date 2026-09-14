---
id: T1634
title: spill 域 offload+evict 生命周期组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1633
created: 2026-09-15
---

## Question

J 会话第 89 轮：spill 生命周期组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpillLifecycleReadoutTest，SpillModule 骨架）：溢出→逐出链路后双读面各自守恒 + 互不串账 + reset 独立。定向 `mvn -pl buzhou-spill -am test -Dtest='SpillLifecycleReadoutTest'` 绿。
