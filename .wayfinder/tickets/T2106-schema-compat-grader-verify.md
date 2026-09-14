---
id: T2106
title: McpSchemaCompatGrader 分级口径的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2105
created: 2026-09-14
---

## Question

如何证明四破坏轴、加法演进与 fail-closed 的判定正确？

## Resolution

**用户常设授权 AFK（可推翻）**

`McpSchemaCompatGraderTest` 九测全绿（`mvn -pl buzhou-mcp -am test`）：同 schema 兼容 / 可选新增兼容 / 属性删除 / 类型翻转 / 既有属性新收必填 / 新增即必填 / 枚举收窄+放宽双向 / 不可解析 fail-closed / 多原因典序（含 newly_required 与 type_changed 并存）。基线 schema 三属性+单必填+枚举，贴近真实工具定义。
