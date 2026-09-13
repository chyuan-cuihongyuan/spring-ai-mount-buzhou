---
id: T1474
title: spill 容量水位读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1473
created: 2026-09-14
---

## Question

J 会话第 12 轮：容量水位读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SpillUsageTest，复用 DiskSpillStoreTest 的 TempDir 骨架；ASCII 内容使 bytes==chars 免编码歧义）：空仓 0/0；两次 store → entryCount=2 且 totalBytes == 两次内容字节和；delete 后回落；usage() 与配额守卫同源（同一 walk 口径）。定向 `mvn -pl buzhou-spill test -Dtest='SpillUsageTest,DiskSpillStoreTest'` 绿。
