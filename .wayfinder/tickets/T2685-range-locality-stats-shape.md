---
id: T2685
title: 范围读局部性分类读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

RangeLocalityStats 的形状怎么裁决？（spec 1742 / effort #1742 / R43）（spec 1742 验收/裁决）

## Resolution

实例面 record(offset, length)（负值忽略）连续性分类（offset==上一读终点=顺序否则随机，首读独立）+census(reads/sequential/random/sequentialShare 可判对<2 −1)——RocksDB 局部性思想。
