---
id: T27
title: memory · 语义边界压缩触发（compact_now 工具）
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T13
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#memory)

## What to build

暴露一个 `compact_now` 工具（MemoryModule 注册），模型可在任务边界/长草稿前自触发压缩；token 阈值保留为安全网（**双触发路径**：质量自触发 + token 兜底）。

## Acceptance criteria

- [ ] 端到端：模型在任务边界调用 `compact_now`，压缩发生且保真
- [ ] token 阈值安全网仍生效（不依赖模型自觉）
- [ ] 既有压缩触发与预算不回归

## Blocked by

无 —— 可立即开工。

## Resolution

已落地（Tier-1）。`CompactNowTool`（buzhou-memory/tool，`compact_now`）：模型在任务边界/长草稿前自触发压缩——把未摘要完成轮折入九段摘要并回报统计（新折入条数/覆盖轮次/代际/token 估算）；幂等（复用 T24 双水位）；**不绕过 T25 对账**（评审修复：同一 reconciler+ledger 挂接）；token 阈值安全网不变（双触发路径）。sessionId 经 ToolContext 注入解析；无绑定会话/无新消息给引导文案。`MemoryModule` 有摘要模型时默认注册（`memory.compact-now-tool` 开关）。spec 01 新增条目。测试：`CompactNowToolTest`（3：折入+统计+幂等 / 无会话引导 / 会话接缝模型自触发）。
