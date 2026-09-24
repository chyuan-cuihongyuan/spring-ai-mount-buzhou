---
id: T6186
title: S 会话 S43 Xor Filter 异或过滤器的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6185]
created: 2026-09-25
---

## Question

S43 合同怎么逐一验绿？（spec 5042 / effort #5042 / S43）

## Resolution

**验证通过**：XorFilterTest 六测全绿——1000 成员零假阴性；
尺寸 1.23×+3 倍数；1 万探针假阳性 <10%；同键集同 checksum；
小集成员全命中；null/空/重复 fail-fast。
