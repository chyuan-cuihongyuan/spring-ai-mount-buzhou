# 1058 — MCP 工具集轮询提供器读面

> 来源：J 会话第 58 轮 = effort #1058（[T1571](../../.wayfinder/tickets/T1571-toolsetpoll-stats-shape.md) / [T1572](../../.wayfinder/tickets/T1572-toolsetpoll-stats-verify.md) / impl 810）。借鉴：etcd watch statistics / Consul 反熵轮次统计（拉取式同步的轮次总量、失败量、变更检出量三面是对账基座）。J 会话首个 mcp 域轮。

## Problem Statement

`DbToolSetProvider`（MCP 工具集 DB 热更新拉取通道，5s 轮询）的 `checkQuietly` 静默吞掉全部 RuntimeException，变更检出与无变更轮次均无任何信号：**轮询健康不可见**。运维改配 MCP 工具集后「为什么不生效」无法回答——是轮询持续失败（DB 连接问题）、还是变更没被检出（equals 语义）、还是根本没轮到下一轮。

## 目标

- `DbToolSetProvider` 增量（mcp，静态面）：五 `AtomicLong`（实施中形状微调：发现 listener 直调 `checkAndFire` 的第二入口——写后免等轮询优化路径，四计数口径会漏账）。
  - 入口桶：`polls`（轮询 checkQuietly）/ `pushRefreshes`（写后 listener 直调）；
  - 结局桶（checkAndFire 汇聚点）：`changesDetected`（清单变更并 fire）/ `unchangedRefreshes`（成功无变更）/ `refreshFailures`（loadAll 抛错，入桶后按原语义抛出——轮询路径吞、直调路径外溢，行为逐位不变）。
- 嵌套 `record ToolSetPollStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**polls + pushRefreshes = changesDetected + unchangedRefreshes + refreshFailures**（每入口恰落一桶）。

## 兼容性

纯增量读面：轮询节奏、变更 fire、异常吞掉（不外溢）语义逐位不变；静态面理由同 R46–R57 先例；无新配置项。

## Out of Scope

- 轮询耗时直方图（周期语义已含节奏）。
- 变更内容 diff 明细（McpDirectoryDiff 另域既有）。
