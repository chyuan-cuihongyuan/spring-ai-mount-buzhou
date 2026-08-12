---
id: T9
title: 撰写「与 Spring AI 原生能力边界」文档（item 6）
type: task
status: open
assignee: ""
blocked-by: [T2]
created: 2026-08-13
---

## Question

基于 [T2](T2-spring-ai-native-vs-buzhou.md) 的逐机制 `NATIVE`/`ADDS`/`REPLACES` 表，落一份面向用户的边界文档：明确——

- **Spring AI 没有的**（Buzhou REPLACES）：Spill 溢出 / read_range、并行工具调用（含超时取消）、Skill 能力 prompt。
- **增强的**（Buzhou ADDS）：记忆压缩（对比社区 `spring-ai-session`）、认知可观测（证据/推理/span event，整合而非另起 OTel 管线）、Hook 护栏（HITL/长产物/事实闭环，建在 Advisor 之上）、持久化 SPI（Summary/SessionState/SessionLease/Observability store）、原子工具集。
- **替代/诚实的**（Buzhou NATIVE = 仅 wrapper）：**MCP 热插拔**——须明确标注 Spring AI 2.0 已原生支持动态工具增删，Buzhou 在此非差异化。

放哪（`docs/spec/` 新增一篇，或 README 单列一节）？中英是否同步？

## Context

- item 6 的实际交付物。事实骨架已由 T2 给出（已闭合），故本 ticket 可直接动手。
- blocked-by [T2](T2-spring-ai-native-vs-buzhou.md)（已闭合 → 已 unblocked → frontier）。
- AFK 可做：照 T2 表组织成文档 + 中英同步 + 链接进 README/spec 索引 + 提交。

## Resolution

<!-- task 完成后填写：文档位置 + 改动 + commit -->
