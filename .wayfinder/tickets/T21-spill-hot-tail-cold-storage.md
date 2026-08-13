---
id: T21
title: spill · hot-tail / cold-storage 两级保留
type: task
status: closed
assignee: "chyuan"
blocked-by: [T20]
epic: T14
created: 2026-08-13
---

**Status:** ready-for-agent（T20 闭合后）· **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#spill)

## What to build

近期 N 条工具结果全量内联（供推理），旧结果溢出至 evidence store、上下文只留自描述占位符。"keep inline" 条数/大小按会话可配。复用 T20 的占位符格式。

## Acceptance criteria

- [ ] 端到端：多次工具调用后，近期结果全量内联、旧结果为自描述占位符
- [ ] 旧结果经回读仍能取回真实切片
- [ ] keep-inline 数/大小可配
- [ ] 不破坏既有溢出/回读语义

## Blocked by

- [T20 · 自描述 Handle + token-aware 阈值](T20-spill-self-describing-handle.md)（复用占位符格式）

## Resolution

已落地（Tier-1）。`HotTailViewProcessor`（`MemoryViewProcessor` 视图级实现）：近期 N 条工具结果全量内联（`hotTail(n)`，存储层 append-only 不动），旧 TOOL 消息超阈值惰性溢出至 SpillStore、视图内替换为 T20 自描述占位符；`hotTailMaxInlineChars` 大小预算（每轮重算总量）；`RuntimeConfig.merge` 升级为多 viewProcessor **链式组合**（spill 先、memory 后，可叠加）；与即时 offload **互斥强制**（启用 hot-tail 自动关 offload，除非显式开启）。spec 02 新增专节。测试：`HotTailViewProcessorTest`（5：内联/溢出/回读/durable/token/预算/幂等）、examples `SpillHotTailIntegrationTest`（三轮会话接缝：近期零损失→旧结果占位符→read_range 真实切片）。
