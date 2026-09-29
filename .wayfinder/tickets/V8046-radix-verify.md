---
id: V8046
title: V 会话 V23 RadixSorter 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8045]
created: 2026-09-30
---

## Question

V23 合同怎么逐一验绿？（spec 8022 / effort #8022 / V23）

## Resolution

**验证通过**：三测全绿——乱序/退化手锚；300 随机 vs
Arrays.sort 圣像+原数组不动；负数/null fail-fast。
