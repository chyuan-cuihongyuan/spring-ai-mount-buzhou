---
id: T6276
title: T 会话 T38 Stable Bloom Filter 稳定布隆过滤器的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6275]
created: 2026-09-26
---

## Question

T38 合同怎么逐一验绿？（spec 6037 / effort #6037 / T38）

## Resolution

**验证通过**：StableBloomFilterTest 五测全绿——1000 近期
插入恒真；20000 噪声流旧成员淡出；重复插入持续可见；双实例
一致；fail-fast（初版噪声流/槽数比失衡淡出不可观测调参钉住）。
