---
id: T1571
title: MCP 工具集轮询提供器读面（ToolSetPollStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1569
created: 2026-09-15
---

## Question

J 会话第 58 轮：mcp 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块轮换）：DbToolSetProvider（DB 工具集热更新轮询，5s 周期）的 checkQuietly 吞掉全部 RuntimeException、变更检出与无变更轮次完全不可见——「改配为什么不生效」（轮询全失败？变更没检出？）无从诊断。etcd watch statistics / Consul 反熵轮次统计思想。

形状裁决：`DbToolSetProvider` 内静态 `AtomicLong` 四计数——polls（checkQuietly 入口）/ pollFailures（RuntimeException 捕获处）/ changesDetected（清单变更 fire）/ unchangedPolls（成功但无变更）；嵌套 `record ToolSetPollStats` + `stats()` + `resetForTest()`。守恒 `polls = changesDetected + unchangedPolls + pollFailures`（每轮恰落一桶）。静态面理由同族先例；行为与返回逐位不变。

Out of scope：轮询耗时直方图（间隔语义已含节奏）；变更内容 diff（McpDirectoryDiff 另域）。
