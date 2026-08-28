# Spec 85 — 错误签名健康面（effort #46）

> wayfinder map：`.wayfinder46/MAP.md`（T327–T328）。#44 fog 毕业生。

## Problem Statement

ErrorSignatures（spec 83）只在进程内有 top()/snapshot()，运维的 `/actuator/buzhou`
快照与 actuator health 面看不到——排障入口缺一角。

## Solution

`ErrorSignaturesHealth implements BuzhouHealth`：mechanism `error-signatures`；
status 恒 UP；details = `{top: ["<sig> x<N>"…top-5], distinct: <在册族数>}`（有界）。
autoconfig 在 BuzhouEndpointConfiguration 内挂 `@ConditionalOnMissingBean` bean
（有 actuator 类才装配）。零新键。

## User Stories

1. 作为运维，我在健康快照一屏看到 top 错误族，所以排障不用加日志。
2. 作为宿主，我要 details 有界，所以健康端点不被刷爆。

## Implementation Decisions

- 恒 UP：观测面不下 DOWN（错误多≠机制失能——BuzhouHealth DOWN 语义纪律）。
- top-5 + "sig xN" 字符串形态（有界且人读）。

## Testing Decisions

- 恒 UP + 有界 top（count 降序）；空表零错误可用；BuzhouHealthEndpoint 聚合含
  error-signatures 段。

## Out of Scope

- 错误签名 OLAP 导出；model 路径签名接线。

## Further Notes

- 与 webhook-outbox / session-index 健康段并列（恒 UP 族——水位告警走指标面）。
