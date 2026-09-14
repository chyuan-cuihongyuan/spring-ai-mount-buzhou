# 1402 — MCP 工具入参 schema 破坏性变更分级

> 来源：L 会话第 3 轮 = effort #1402（票 T2105 / T2106 / impl 1055）。借鉴：buf breaking（protobuf schema 演进的破坏性变更机判——「升级是否 break 调用方」从肉眼对 diff 变成分级结论）。

## Problem Statement

MCP server 升级后工具入参 schema 可能破坏既有调用方（模型侧与宿主侧的旧调用形态失效）。`McpDirectoryDiff`（spec 822 同源/600 对比口径）**显式把入参 schema 划出对比范围**（只比 title + 三 hint）——schema 级变更今天完全不可见：属性被删/类型翻转/枚举收窄全靠人肉对 JSON。

## 目标

- `McpSchemaCompatGrader`（mcp，纯函数静态面）：`grade(oldSchemaJson, newSchemaJson)` → 嵌套 `record SchemaCompatVerdict(CompatClass compatClass, List<String> reasons)`。
  - **判定视角 = 既有调用方守恒**：`removed_property` / `type_changed` / `newly_required`（含新增即必填）/ `enum_narrowed` 判 BREAKING；可选属性新增、枚举放宽、无结构变化判 COMPATIBLE；
  - 解析失败 fail-closed 判 BREAKING（`unparseable_schema_fail_closed`——未知不冒充安全，SsrfGuard 同哲学）；
  - reasons 稳定典序去重（判定可快照 diff）。
- 闭集 `enum CompatClass { COMPATIBLE, BREAKING }`。

## 兼容性

纯函数零 IO，不触 registry/store/connection；无配置项。只读顶层 properties/required/enum/type 四键——嵌套结构与数值约束方向（min/max 收紧放宽）不判定（诚实入档 Out of Scope）。

## Out of Scope

- 嵌套对象递归对比（顶层四键口径显式；递归留后续按需）。
- min/maxItems、min/maxLength 等数值约束收紧判定（方向语义易误判，留后续）。
- registry 轮询接线与台账化（分级判定与 822 快照/814 台账的装配协作留后续轮）。
- response schema 侧（MCP 工具出参无 schema 契约，无从分级）。
