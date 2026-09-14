---
id: T1642
title: offload→readBack 双轴闭环组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1641
created: 2026-09-15
---

## Question

J 会话第 93 轮：双轴闭环如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（OffloadReadBackComboTest，SpillModule 骨架）：溢出→回读闭环后 SpillOffloadStats 与 ReadRangeStats 各自守恒 + offloaded 与 reads 闭环对应。定向 `mvn -pl buzhou-spill -am test -Dtest='OffloadReadBackComboTest'` 绿。
