---
id: T5074
title: Q 会话 R37 Elias gamma 的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5073]
created: 2026-09-18
---

## Question

R37 合同怎么逐一验绿？（spec 3036 / effort #3036 / R37）

## Resolution

**验证通过**：EliasGammaCodecTest 五测全绿——手算码字五例（1/2/
3/4/8）、1..1000 全往返+码长=串长、四值流连解游标推进至串尾、
幂律紧凑（1/31/63 → 1/9/11 位）、六路 fail-fast。
