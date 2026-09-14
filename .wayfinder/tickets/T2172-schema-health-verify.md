---
id: T2172
title: 四态分桶与裸奔率派生的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2171
created: 2026-09-14
---

## Question

如何证明四态分桶与校验器跳过口径同源？

## Resolution

**用户常设授权 AFK（可推翻）**

`ToolSchemaHealthAuditTest` 五测全绿（`mvn -pl buzhou-core -am test`）：合规 schema（含仅 required 型）VALID；MISSING×2/UNPARSEABLE×1 分桶+findings+bypassRatio=1.0；非 object 与空 {} 三键全缺=NOT_OBJECT 裸奔；findings 封顶 16 超出只计数；空清单 -1 哨兵。评审修正：Spring AI builder 拒绝空 schema——测试改 ToolDefinition 接口直实现表达缺失态。
