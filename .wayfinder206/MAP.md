# Wayfinder Map — Buzhou 工具目录指纹（effort #206，B 会话第 29 轮）

> B 会话第 29 轮。主题池轮换：MCP 工具集有漂移检测（spec 18），但装配面整体
> 工具目录（内置+MCP+宿主）缺一个可对账的指纹——「今天模型能用的工具和上周
> 差在哪」不可答。借鉴 SBOM/锁单（inventory diff）思想。

## Destination

ToolCatalogFingerprint（core/exec）：from(List<ToolDefinition>) →
name→schema sha256 指纹表 + 整体摘要；diff(other) → 三分类
ADDED/REMOVED/CHANGED——工具面变更可对账可审计。

## Notes

- 号段：B=奇数 spec（本轮 175）；轮次 .wayfinder200+。
- 与 MCP drift（18，协议单源）互补：这是装配面全景快照。

## Decisions so far

- schema 规范化（strip）后哈希——空白差不构成变更。

## Not yet specified

- 快照持久化/导出 JSONL；装配期自动对账告警。

## Out of scope

- 沿用各轮；语义级 schema 等价判定。

## Tickets

- [x] [T545 ToolCatalogFingerprint（指纹表+摘要+diff）](tickets/T545-catalog-fp.md)（impl-301）
- [x] [T546 指纹回归（指纹稳定/空白等价/三分类/空目录）](tickets/T546-catalog-fp-tests.md)（impl-301）
