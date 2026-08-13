---
id: T22
title: spill · per-tool durable override（永不溢出 / 超 X 才溢出）
type: task
status: closed
assignee: "chyuan"
blocked-by: [T20]
epic: T14
created: 2026-08-13
---

**Status:** ready-for-agent（T20 闭合后）· **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#spill)

## What to build

工具/Hook 可声明「永不溢出」或「超 X 才溢出」（对截断敏感的输出如 DB schema、整文件）。复用 T20 的阈值/占位符机制。

## Acceptance criteria

- [ ] 声明 durable 的大输出保持全量内联（不溢出/不截断）
- [ ] 「超 X 才溢出」按声明执行
- [ ] 未声明工具走默认阈值
- [ ] 不破坏既有溢出语义

## Blocked by

- [T20 · 自描述 Handle + token-aware 阈值](T20-spill-self-describing-handle.md)（复用阈值机制）

## Resolution

已落地（Tier-1）。工具策略键 `spillNeverOffload: true`（永不溢出，截断敏感输出如 DB schema/整文件保持全量内联）在即时溢出（`SpillOffloadHook`）与视图级溢出（`HotTailViewProcessor`）双路径生效；「超 X 才溢出」复用 T20 的 per-tool token/chars 覆盖；未声明工具走全局默认。策略判定收敛到 `SpillThresholds`（单一事实源）。spec 02 新增专节。测试：`HotTailViewProcessorTest.durableDeclarationNeverSpillsEvenWhenAged` / `perToolThresholdTokensAppliesToAgedResults`、`SpillOffloadHookTest.perToolThresholdOverrideViaToolPolicies`（既有）。
