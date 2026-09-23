---
id: T6091
title: R 会话 R46 Avro 读写模式解析的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-24
---

## Question

结构化数据跨版本读写怎么确定性解析？（spec 4045 /
effort #4045 / R46）

## Resolution

**SchemaEvolution（core/policy）**：Avro resolution 思想——
名/alias 配对 + 加宽白名单（int→long/float/double 等）+ 读方
默认值补位显形 + 写方多余字段 dropped 读数；不兼容 fail-fast
带字段名；Plan 结构化确定性。
