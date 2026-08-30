# Spec 175 — 工具目录指纹（effort #206）

> wayfinder map：`.wayfinder206/MAP.md`（T543–T544）。借鉴：SBOM / 锁单
> inventory diff——依赖面变更是审计第一问。

## Problem Statement

模型可用的工具面（内置原子工具 + MCP 热插拔 + 宿主注册）随版本与热更新漂移：
某天多了个 `run_command`、某工具 schema 悄悄改了字段——没有可对账的快照，
「今天与上周差在哪」无法回答；工具面是 agent 的权限面，漂移不可见即风险不可见。

## Solution

`ToolCatalogFingerprint`（core/exec）：

- **构建**：`of(List<ToolDefinition>)` → per 工具 `name → sha256(strip(schema))`
  指纹表 + 整体摘要（对全表哈希——一条串代表一版目录）。
- **对账**：`diff(other)` → `Diff(added, removed, changed)` 三分类（changed =
  同名指纹异）；`summaryHex()` 入日志/工单做版本对账锚。
- 用法：装配期/定时/事件触发各拍一次快照存档；任意两版 diff 即审计报告。

## User Stories

1. 作为审计员，升版前后各拍快照 diff——新增/移除/变更工具一目了然。
2. 作为运维，summaryHex 写进部署记录——任何时点可答「当时工具面是哪版」。
3. 作为宿主，MCP 热更新后 diff 确认变更集与预期一致（意外工具混入即显形）。

## Implementation Decisions

- strip 后哈希（空白差不构成变更——与 argsHash 同纪律）。
- diff 只比 name+指纹（description 变更不计——描述是文档不是契约）。

## Testing Decisions

- 同目录两次构建指纹与摘要一致（稳定性）；schema 空白差等价；diff 三分类
  各正确；空目录/空 diff 边界；description 变更不计入 changed。

## Out of Scope

- 语义等价 schema 判定；快照持久化；自动告警接线。

## Further Notes

- 漂移双面：MCP 协议单源（18）+ 装配面全景（本轮）。
