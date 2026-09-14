# 1058 — MCP 工具集轮询提供器读面

> 来源：J 会话第 58 轮 = effort #1058（[T1571](../../.wayfinder/tickets/T1571-toolsetpoll-stats-shape.md) / [T1572](../../.wayfinder/tickets/T1572-toolsetpoll-stats-verify.md) / impl 810）。借鉴：etcd watch statistics / Consul 反熵轮次统计（拉取式同步的轮次总量、失败量、变更检出量三面是对账基座）。J 会话首个 mcp 域轮。

## Problem Statement

`DbToolSetProvider`（MCP 工具集 DB 热更新拉取通道，5s 轮询）的 `checkQuietly` 静默吞掉全部 RuntimeException，变更检出与无变更轮次均无任何信号：**轮询健康不可见**。运维改配 MCP 工具集后「为什么不生效」无法回答——是轮询持续失败（DB 连接问题）、还是变更没被检出（equals 语义）、还是根本没轮到下一轮。

## 目标

- `DbToolSetProvider` 增量（mcp，静态面）：四 `AtomicLong`。
  - `polls`：checkQuietly 入口计数（总桶）；
  - `changesDetected`：清单变更并 fire 变更监听；
  - `unchangedPolls`：成功但无变更；
  - `pollFailures`：轮询 RuntimeException 捕获处。
- 嵌套 `record ToolSetPollStats(long polls, long changesDetected, long unchangedPolls, long pollFailures)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**polls = changesDetected + unchangedPolls + pollFailures**（每轮恰落一桶）。

## 兼容性

纯增量读面：轮询节奏、变更 fire、异常吞掉（不外溢）语义逐位不变；静态面理由同 R46–R57 先例；无新配置项。

## Out of Scope

- 轮询耗时直方图（周期语义已含节奏）。
- 变更内容 diff 明细（McpDirectoryDiff 另域既有）。
