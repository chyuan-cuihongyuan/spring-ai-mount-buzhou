---
id: T6263
title: T 会话 T32 External Merge Sort 外归并排序的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

超内存数据怎么排序不 OOM？（spec 6031 /
effort #6031 / T32）

## Resolution

**ExternalMergeSort（core/fs，源码 T30 预载）**：⌈n/chunk⌉
游程窗内排序+多路归并逐位取最小（并列游程序靠前）；
sorted/runCount/chunkSize 读数；null/窗≤0 fail-fast。
