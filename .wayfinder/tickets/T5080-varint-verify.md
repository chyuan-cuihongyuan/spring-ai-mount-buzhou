---
id: T5080
title: Q 会话 R40 varint 的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5079]
created: 2026-09-18
---

## Question

R40 合同怎么逐一验绿？（spec 3039 / effort #3039 / R40）

## Resolution

**验证通过**：VarintCodecTest 五测全绿——zigzag 手算映射双向、
字节长度阶梯（1/1/2/7/10）、−1000..1000 与七极值全往返、五值流
游标连解至串尾、截断/残留/越界/超宽四路 fail-fast。
