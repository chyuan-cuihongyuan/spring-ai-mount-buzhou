# 840 — MCP 建连遥测读数

> 来源：H 会话第 41 轮 = effort #840 / [T1181](../../.wayfinder/tickets/T1181-mcp-connect-telemetry.md) / [T1182](../../.wayfinder/tickets/T1182-mcp-connect-telemetry-verify.md) / impl 593。
> 借鉴：gRPC channelz。

## Problem

MCP server 建连失败只有 retry 日志：「连不上 vs 连上慢、哪个 server 在拖后腿」无样本面。

## Solution

`McpConnectTelemetry`（mcp，纯读数）：

- **per-server**：成败计数+连续失败 streak+lastDuration（负值=未知保留旧值）+近窗 16 成功率。
- **worstFirst()**：失败数降序——问题 server 排前。
- **有界**：server 封顶 32+truncated。

## 兼容性

纯新增（喂点=工厂/注册表装配侧）；连接 seam 零变更。

## 诚实边界

耗时口径由工厂定；streak 无惩罚；读数不重连。
