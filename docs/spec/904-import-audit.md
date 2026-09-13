# 904 — 导入审计与严格模式

> 来源：I 会话第 5 轮 = effort #904（[T1259](../../.wayfinder/tickets/T1259-import-strict-shape.md) / [T1260](../../.wayfinder/tickets/T1260-import-strict-verify.md) / impl 657）。借鉴：pg_restore `--exit-on-error` / protobuf [unknown fields](https://protobuf.dev/programming-guides/proto3/#unknowns) 处置策略——「宽松消费」与「严格审计」分开给。

## Problem Statement

`SessionExport` 标注 `@JsonIgnoreProperties`：未知顶层字段静默丢弃。上游版本新增字段（如未来 v2 扩展）在旧消费端导入「成功」但字段丢失——静默数据丢失。protobuf 以 unknown fields 保留策略解决；pg_restore 以 --exit-on-error 给严格档。本仓两者皆无：导入前无法知道「这份文档里有没有我不认识的东西」。

## 目标

- 新公共纯函数类 `SessionExportAudit`（core.session）：
  - `audit(String exportJson)`：Jackson 读树（宽松，不抛未知字段错误）对比已知顶层组件（format/version/sessionId/appId/agentName/exportedAtEpochMs/messages/summary/state/extensions）→ 公共 record `AuditReport(List<String> unknownTopLevelFields, List<String> missingRecommendedFields, boolean strictCompatible)`；
  - 推荐字段 = sessionId、exportedAtEpochMs、messages（字段缺失或 null 即入报告；空消息数组是合法状态——SessionExport.of 允许 0 消息，不入报告）；
  - `fromJsonStrict(String exportJson)`：audit 不过（strictCompatible=false）抛 `SessionImportException`（含明细）；通过则等价 `SessionExport.fromJson`；
- 既有 `fromJson` / `importSession` 零变化（宽松路径向后兼容保留）；
- 审计只读不落盘（纯函数纪律）。

## 兼容性

纯增量：新公共类型 + 新静态入口；宽松路径行为零变化。
