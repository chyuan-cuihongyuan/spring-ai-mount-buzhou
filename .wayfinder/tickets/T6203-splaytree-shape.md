---
id: T6203
title: T 会话 T2 Splay Tree 伸展树的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-25
---

## Question

有序映射怎么不加平衡元数据也保摊还对数？（spec 6001 /
effort #6001 / T2）

## Resolution

**SplayTree（core/concurrent）**：Sleator-Tarjan 思想——
访问即伸展（zig/zig-zag/zig-zig 三式旋至根），热键自动上浮
（摊还 O(log n)）；put upsert/get null 缺席/remove 嫌席
fail-fast；size/rootKey 读数 + keysInOrder 中序导出。
