---
id: T1644
title: compact×evidence 交叉组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1643
created: 2026-09-15
---

## Question

J 会话第 96 轮：交叉组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（CompactEvidenceComboTest，BuzhouStores 骨架）：压缩前后 evidence 回查命中一致 + 双读面各自守恒。定向 `mvn -pl buzhou-memory -am test -Dtest='CompactEvidenceComboTest'` 绿。
