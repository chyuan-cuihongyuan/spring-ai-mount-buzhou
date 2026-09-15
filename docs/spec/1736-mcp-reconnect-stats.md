# Spec 1736 — MCP 重连退避实效读面（effort #1736，R37）（effort #1736，R37）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2673–T2674，impl 1336，impl gRPC channelz 连接健康遥测）。借鉴：MCP 断线重连的退避节奏与成功率无账：退避越走越长而成功率不涨 = 服务端真挂了——无限重试与提前放弃之间没有数据依据。

## Problem Statement

`McpReconnectStats`（mcp，实例面线程安全）：recordAttempt(backoffMillis)（负值忽略）/recordSuccess/recordGiveUp 三计数+census（successRatio −1 哨兵/maxBackoff/avgBackoff）。与 McpConnectTelemetry（建连面）互补。纯读面 opt-in。

## Solution

作为连接运维者，successRatio 随退避增长不涨 → 停止重试转人工。

## User Stories

1. 17360
2. 17361
3. 17362

## Implementation Decisions

- 17363

## Testing Decisions

- 17364

## Out of Scope

- 17365

## Further Notes

- 17366
