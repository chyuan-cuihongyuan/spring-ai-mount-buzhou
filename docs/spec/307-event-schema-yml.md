# Spec 307 — 事件 schema yml 声明（effort #307）

> wayfinder map：`.wayfinder307/MAP.md`（T605–T606）。借鉴：JSON Schema
> required（spec 209 原语装配收尾——fog 227「事件 schema 声明 yml 化」项）。

## Problem Statement

事件 payload 契约（per-type 必备键，209）只能编程声明：yml 面缺失，运维
调契约要改代码；装配面也没有把 checker 包进投递链——原语 standalone。

## Solution

`buzhou.webhook.schema` 属性组：

- `required-keys.<type>=k1,k2`（Map 绑定；空/未配置 = 不装配 checker——零变化）
- `fail-open=true`（观察模式：违规放行 + 计数，调查期用）

装配：checker bean 包装 forwarder（NullBean 语义——空声明返回 null，类型
收集自动跳过）；全局挂点去重——被 checker 包装的 delegate 不再直挂（防
同一事件双投）。未声明类型放行（209 open-world 不变）。

## User Stories

1. 作为运维，yml 一行声明某事件类型的必备键——缺键事件不再污染下游。
2. 作为运维，fail-open 先观察违规量再收紧——灰度式契约收紧。

## Implementation Decisions

- 去重逻辑抽 `effectiveGlobalListeners(List)` 纯函数（可单测）。
- checker 只在 forwarder 存在时装配（@ConditionalOnBean）。

## Testing Decisions

- `EventSchemaYmlAssemblyTest`：yml Map 绑定（含点号类型键）；fail-closed
  坏事件不投/好事件投（本地 HttpServer 收件）；fail-open 违规也投；去重
  纯函数（checker 在场 → delegate 去掉，独立监听保留）；默认无 checker。

## Out of Scope

- 信封层 schema（20）；closed-world 类型注册制。

## Further Notes

- 投递链现状：schema（307 最外拦坏）→ dedup（203 吞重）→ redactor（177
  出站脱敏）→ fanout（151）——链序定案于 spec 211 E2E。
