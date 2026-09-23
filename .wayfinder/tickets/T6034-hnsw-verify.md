---
id: T6034
title: R 会话 R17 HNSW 的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6033]
created: 2026-09-23
---

## Question

R17 合同怎么逐一验绿？（spec 4016 / effort #4016 / R17）

## Resolution

**验证通过**：HnswBeamSearchTest 四测全绿——百点网格四探针 top1
精确 + top5 与暴力基准集合相等；分批增量召回保持 + maxLevel 读数；
k 超规模返全量；空索引诚实 + 畸形七型 fail-fast。首版增量用例
对账基准 TreeSet（id 序）与距离序混比已改集合口径。
