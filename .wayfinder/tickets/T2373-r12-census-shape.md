---
id: T2373
title: R12 孤类普查与熔断遥测接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2372
created: 2026-09-15
---

## Question

N 会话第 12 轮：孤类普查后全修还是入档+分批修？

## Resolution

选 **入档 + 分批修**。15 项分三个家族（guard hook 装配面 / core 读数喂点 /
resilience 遥测），每项独立纵切片。本轮先修 resilience 域两项（喂点最明确：
ModelCircuitBreaker 变迁与半开路径）——withTelemetry 链式注入避免构造器再膨胀，
装配恒挂对齐 702 journal 先例（纯读数、有界内存）。
