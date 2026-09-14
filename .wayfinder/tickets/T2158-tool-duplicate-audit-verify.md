---
id: T2158
title: 重名组显形与目录健康判定的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2157
created: 2026-09-14
---

## Question

如何证明重名组显形、计数与排序？

## Resolution

**用户常设授权 AFK（可推翻）**

`ToolCatalogDuplicateAuditTest` 五测全绿（`mvn -pl buzhou-core -am test`）：空清单健康哨兵；唯一目录健康；read_file×2 重名组显形（total 4/distinct 3）；多组名典序（a_tool×3 前于 z_tool×2）；本地/MCP 同名真实场景模拟。
