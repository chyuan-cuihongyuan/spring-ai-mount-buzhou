---
id: T6166
title: S 会话 S33 B+ Tree 有序索引的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6165]
created: 2026-09-24
---

## Question

S33 合同怎么逐一验绿？（spec 5032 / effort #5032 / S33）

## Resolution

**验证通过**：BPlusTreeTest 五测全绿——顺序 64 键全序可查；
upsert 不增位；扰动 500 插入 vs TreeMap 圣像全等（等键
childIndexOf 回归钉住）；树高单叶=1/负载 ≥2≤5；fail-fast。
