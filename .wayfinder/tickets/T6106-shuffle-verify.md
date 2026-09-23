---
id: T6106
title: S 会话 S3 Fisher-Yates 无偏洗牌的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6105]
created: 2026-09-24
---

## Question

S3 合同怎么逐一验绿？（spec 5002 / effort #5002 / S3）

## Resolution

**验证通过**：FisherYatesShuffleTest 五测全绿——均匀性（六
排列 6000 种子频次 ±25%）；双射排列；同种子同排列重放；
新列表变体不触原列表；null/负 size fail-fast。
