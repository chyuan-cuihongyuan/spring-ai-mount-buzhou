---
id: U7210
title: U 会话 U5 RunLengthCodec 行程编码的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7209]
created: 2026-09-29
---

## Question

U5 合同怎么逐一验绿？（spec 7004 / effort #7004 / U5）

## Resolution

**验证通过**：RunLengthCodecTest 四测全绿——300 随机
roundtrip；600 切 3 对逐字节钉住；全相异膨胀/全重复压缩
读数；fail-fast。
