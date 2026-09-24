---
id: T6185
title: S 会话 S43 Xor Filter 异或过滤器的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

静态成员判定怎么比 Bloom 更省更少探针？（spec 5042 /
effort #5042 / S43）

## Resolution

**XorFilter（core/metrics）**：Graf-Lemire 思想——三散列三槽
+8 位指纹+异或查询（单次数组访问级）；剥洋葱构建失败换盐
重试（确定性）；成员恒真无假阴性；checksum 确定性审计读数；
null/空/重复 fail-fast。
