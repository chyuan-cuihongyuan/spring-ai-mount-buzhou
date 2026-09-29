---
id: V8034
title: V 会话 V17 Base58Codec 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8033]
created: 2026-09-30
---

## Question

V17 合同怎么逐一验绿？（spec 8016 / effort #8016 / V17）

## Resolution

**验证通过**：三测全绿——Bitcoin 官方向量；前导零多字节；
300 随机 roundtrip；非法字符位置 fail-fast。
