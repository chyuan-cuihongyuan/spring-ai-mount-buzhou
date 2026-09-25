---
id: T6274
title: T 会话 T37 Counting Bloom Filter 计数布隆过滤器的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6261]
created: 2026-09-26
---

## Question

T37 合同怎么逐一验绿？（spec 6036 / effort #6036 / T37）

## Resolution

**验证通过**：CountingBloomFilterTest 五测全绿——500 插入
零假阴性；删除即缺席；重复计数守恒；假阳性 ≤5% 经验界；
fail-fast。
