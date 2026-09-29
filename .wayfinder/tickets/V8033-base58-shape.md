---
id: V8033
title: V 会话 V17 Base58Codec 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

二进制标识怎么人工可转录？（spec 8016 / effort #8016 / V17）

## Resolution

**Base58Codec（core/message）**：Bitcoin 58 字符表大整数
进制转换；前导 0x00→'1' 约定；decode 非法字符携带位置
fail-fast；roundtrip 全等。
