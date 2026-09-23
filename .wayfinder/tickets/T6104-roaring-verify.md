---
id: T6104
title: S 会话 S2 Roaring 压缩位图的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6103]
created: 2026-09-24
---

## Question

S2 合同怎么逐一验绿？（spec 5001 / effort #5001 / S2）

## Resolution

**验证通过**：RoaringBitSetTest 五测全绿——固定种子圣像对拍
（contains/cardinality 全等）；密疏容器转换阈值显证；and/or
基数守恒；负值/越域 fail-fast；确定性回放。
