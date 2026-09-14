# 1435 — 工具 schema 健康审计

> 来源：L 会话第 36 轮 = effort #1435（票 T2171 / T2172 / impl 1088）。借鉴：ajv / OpenAPI schema 校验（schema 本身的健康度是校验生效的前提——schema 损坏的校验等于没有校验）。

## Problem Statement

`ToolArgsValidator` 的 permissive 决策（spec 12：schema 缺失/不可解析/非 object 时**跳过校验放行**）意味着「工具 schema 坏了」=「该工具裸奔无校验」——但有多少工具在裸奔无审计面：schema 缺失（Spring AI builder 层面已拦，但手写 ToolDefinition 可达）、非法 JSON、三键全缺（properties/required/type 全空校验器直接跳过）静默。

## 目标

- `ToolSchemaHealthAudit`（core/exec，纯函数静态面，private 构造）：
  - `analyze(List<ToolCallback>)` → `record Report(totalTools, valid, missing, unparseable, notObject, findings)`；
  - 四态闭集 `enum SchemaState { VALID, MISSING, UNPARSEABLE, NOT_OBJECT }`——**与 ToolArgsValidator 跳过条件严格同口径**（properties/required/type 全缺=NOT_OBJECT 裸奔）；
  - `bypassRatio()` 派生（非 VALID 占比；0 工具哨兵 -1）；
  - findings 非 VALID 明细有界封顶 16（基数纪律）。
- 纯函数零状态：不触装配/注册（修复归宿主）。

## 兼容性

纯函数零 IO；审计只读不裁决。

## Out of Scope

- schema 深度质量检查（必填属性覆盖度等——语义域）。
- 装配期 fail-fast（行为变更另轮）。
- MISSING 态在 Spring AI builder 下不可达的说明（builder hasText 拦截；手写 ToolDefinition 可达——口径仍保留）。
