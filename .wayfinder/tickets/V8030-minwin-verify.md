---
id: V8030
title: V 会话 V15 MinWindowSubstring 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8029]
created: 2026-09-30
---

## Question

V15 合同怎么逐一验绿？（spec 8014 / effort #8014 / V15）

## Resolution

**验证通过**：三测全绿——BANC 经典手锚+并列取最左；300
随机 vs 暴力 O(n²m) 圣像；fail-fast。
