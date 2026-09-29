---
id: V8010
title: V 会话 V5 GlobMatcher 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8009]
created: 2026-09-29
---

## Question

V5 合同怎么逐一验绿？（spec 8004 / effort #8004 / V5）

## Resolution

**验证通过**：三测全绿——手锚八例（含首 ] 字面量/空串）；
300 随机 vs 递归暴力圣像；fail-fast（null/未闭合 [）。
