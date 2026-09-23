---
id: T6019
title: R 会话 R10 Crockford Base32 的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

口传/工单 ID 的人类可转录编码怎么做？（spec 4009 / effort #4009 / R10）

## Resolution

**CrockfordBase32（core/message，纯静态）**：Crockford 2001——
32 符号字母表剔 I/L/O/U + 解码宽容归一（O→0、I/L→1、大小写不
敏感、连字符忽略）；可选 mod-37 校验符号（37 素数>32 单字符检错）。
与 Snowflake 位布局正交：本件管人面转录。
