---
id: T5006
title: Q 会话 R3 并查集的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5005]
created: 2026-09-18
---

## Question

R3 合同怎么逐一验绿？（spec 3002 / effort #3002 / R3）

## Resolution

**验证通过**：DisjointSetTest 九测全绿——单例初态（find/size/
connected 各自反身）、链式传递闭包、冗余+自合并 false 计数不动、
计数 5→3→3→2→1 只随有效合并递减、sizeOf 链聚合（4/4/1）、反复
find 压缩后稳定同根、双组件互斥、越界（−1/3/99）与容量 0
fail-fast。只测外部行为不测树形（秩是实现细节）。
