---
id: T3
title: core/memory/spill/guard "做深做透"的验收基线（Definition of Done）
type: grilling
status: open
assignee: ""
blocked-by: [T2]
created: 2026-08-13
---

## Question

对 core / memory / spill / guard **各自**，「做深做透」到什么程度算 done？绿测试之上还要什么——

- 属性测试 / 不变式？
- 故障注入（崩溃续跑 / 工具超时 / 去重 / 幂等）？
- 预算压力下的压缩正确性（信息不断崖丢失）？
- read_range 字节区间 / jsonpath / 分页 三种回读的正确性边界？
- HITL → state → attachment 事实闭环的正确性？
- 内部 SPI 契约稳定到可冻结 `api` 子包？

这是目的地的**量化锚**：定了它，per-module 深度 ticket 才能 graduate（见 MAP「Not yet specified」）。

## Context

- 须避免与 Spring AI 原生重复，故 **blocked-by [T2](T2-spring-ai-native-vs-buzhou.md)**：知道原生已有什么，才知道 Buzhou 该把哪条线做到多深。
- 这张是 grilling（HITL）：需要用户逐模块给「深」的判据，agent 不能代答。

## Resolution

<!-- grilling 后填写：四模块各自的 DoD 清单，并据此从 MAP Not yet specified graduate 出 per-module 深度 ticket -->
