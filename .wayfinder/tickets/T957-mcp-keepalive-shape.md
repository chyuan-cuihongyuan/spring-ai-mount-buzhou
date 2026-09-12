---
id: T957
title: MCP keepalive 空闲探活的形态裁决（Retry-After ruled-out 后顺延）
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

原列主题「429/503 Retry-After 尊重退避」缺口核查已被 spec 10 既有实现完整覆盖（DefaultErrorClassifier 解析 Retry-After 头 → ResilienceAdvisor 钳制尊重）——ruled-out 顺延。MCP 长连接空闲期死掉（server 重启/网络断）要到下一次工具调用才暴露——把失败延迟到用户 Turn 内。gRPC keepalive 式空闲探活怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 4 轮 = effort #703 / spec 703 / impl 506）：DefaultMcpClientRegistry 增 opt-in `keepaliveInterval`（null/零 = 关，默认零行为变化）——注册表既有 scheduler 上 `scheduleWithFixedDelay` 周期探活：对每条 ACTIVE 连接调 `listToolNames()`（轻量 RPC，与漂移基线 spec 18 同源，伪连接默认空表无开销）。成功：`buzhou.mcp.keepalive.ok`（tag server）+ 计数；失败：`buzhou.mcp.keepalive.failed` + WARN + **重建该条目**（与 refresh 的 spec-changed 路径同口径：markDraining 引用计数排空 + addEntry 原样重建，refreshLock 内做——不与并发 refresh 竞态；条目已被摘除则不动作）。探活失败计数 getter 供健康/编程面。借鉴 gRPC keepalive pings（空闲连接活性探测）+ nginx upstream max_fails 探测思想。
