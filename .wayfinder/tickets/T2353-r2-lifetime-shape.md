---
id: T2353
title: R2 MCP 连接最大寿命的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2352
created: 2026-09-15
---

## Question

N 会话第 2 轮：长连接腐化防护选哪种形状——到寿退役（maxLifetime）还是健康度动态重建？

## Resolution

选 **HikariCP maxLifetime 到寿退役**。理由：健康度动态重建依赖探活信号（已有 keepalive，
失败即重建——信号路径已存在），而「无症状腐化」（server 端句柄漂移、TLS 会话老化、
工具基线失真）恰恰是探活发现不了的，只有静态寿命兜底。退役复用 spec 703 探活失败的
`rebuildEntry` 口径（排水+原样重建），在飞推迟（归还时退役语义，不硬切）。
推演注：不引入 HikariCP 的 ±30s 退役抖动——server 数量级小且排水宽限天然错峰。
