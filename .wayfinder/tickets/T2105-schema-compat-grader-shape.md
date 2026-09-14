---
id: T2105
title: MCP 工具入参 schema 破坏性变更分级（McpSchemaCompatGrader）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 3 轮：schema 演进兼容性判定的形状与口径选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：McpDirectoryDiff（822 同源）显式把入参 schema 划出对比口径（title+三 hint）——schema 级变更为其声明的留白延伸；mcp 侧无既有 CompatGrader/SchemaDiff。

形状裁决：`McpSchemaCompatGrader` 纯函数静态面（grade(String,String)）——客户端守恒视角四破坏轴（removed_property/type_changed/newly_required/enum_narrowed）+ 加法演进 COMPATIBLE + 解析失败 fail-closed BREAKING；嵌套 SchemaCompatVerdict(compatClass, reasons) reasons 典序去重；顶层四键口径显式（嵌套/数值约束 Out of Scope 诚实入档）。纯函数零 IO，registry 接线留后续轮。

Out of scope：嵌套递归；数值约束方向；接线台账化；response 侧。
