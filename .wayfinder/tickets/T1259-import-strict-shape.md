---
id: T1259
title: 导入审计与严格模式的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 5 轮：会话导出导入已有校验和验证（spec 70/710/733）——「导入严格模式」（pg_restore --exit-on-error / protobuf unknown fields 思想）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 5 轮 = effort #904 / spec 904 / impl 657）：缺口成立——`SessionExport.fromJson` 标注 `@JsonIgnoreProperties`（未知顶层字段静默丢弃），导入成功≠文档无损：上游新版本多出的字段在旧消费端静默消失（数据丢失不可见）。落点 core.session 新公共纯函数类 `SessionExportAudit`：① `audit(json)` 读树对比已知顶层组件 → `AuditReport(unknownTopLevelFields, missingRecommendedFields, strictCompatible)`（只读不改宽松路径）；② `fromJsonStrict(json)` opt-in——审计不过（有未知字段或缺失推荐字段）抛既有 `SessionImportException`，通过则走宽松 `fromJson`。推荐字段=sessionId/exportedAtEpochMs/messages（缺失即不可用或不可追溯）。既有 fromJson/importSession 零变化。
