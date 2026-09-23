---
id: T6068
title: R 会话 R34 语义化版本序的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6067]
created: 2026-09-23
---

## Question

R34 合同怎么逐一验绿？（spec 4033 / effort #4033 / R34）

## Resolution

**验证通过**：SemVerOrderTest 五测全绿——官方链八级全链有序；
build 等价；1.0.10 > 1.0.9 数值段反字典序 + 数字段 < 字母段；
无 pre > 有 pre；非法五型 fail-fast（缺段/前导零/空 pre/
非法字符/v 前缀）。
