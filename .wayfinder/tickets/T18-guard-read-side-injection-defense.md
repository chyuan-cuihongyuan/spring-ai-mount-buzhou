---
id: T18
title: guard · 读侧注入防御（spotlighting + canary 泄漏检测）
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T15
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#guard)

## What to build

读侧 offload 把工具/RAG 输出回灌 prompt 时，用**随机分隔符 + 交织标记字符**包裹（delimiting + datamarking），system prompt 指示模型把标记段当**纯数据**。同时在 prompt 注入密语，`afterTool` 检测密语是否泄漏进工具输出；泄漏则阻断该调用并把该输入 embedding 入拒识向量（**自硬化**）。与既有「读侧失败降级透传」正交——本片隔离的是**内容可信度**，非失败处理。

## Acceptance criteria

- [ ] 端到端：工具输出含注入载荷时，写侧工具调用未被影响（canary 未泄漏）
- [ ] 注入载荷被标记为「仅数据」，模型不执行其中伪指令
- [ ] canary 泄漏被检出并阻断 + 自硬化（变体再次出现被拦）
- [ ] 正常工具输出回读/回灌行为不回归（`HookChainTest`/`LongContentGuardEndToEndTest` 绿）

## Blocked by

无 —— 可立即开工。（与 T19 同在 Hook 链，建议串行以免冲突。）

## Resolution

已落地（Tier-1）。`SpotlightHook`（order 80，先于 spill）：随机分隔符 + 「仅数据」告示 + 交织标记字符（短内容逐字符、超长降频）包裹外部输出；幂等；可信框架文本（canary 拦截告示）不包裹。`CanaryGuardHook`（order 70）：beforeModel **前置**注入密语（append 会破坏工具循环输入形状——评审中实测发现并修正）；afterTool 检漏 → 拦截 + 拒识记忆（会话状态）→ 变体自硬化（字符 n-gram Jaccard >=0.6，Tier-1 的 embedding 近似）。格式单一事实源 `Spotlighting`（core.hook），spill 侧 unwrap 还原后判定/落盘（评审修复的 T18×T20 交互缺陷）。`GuardModule.Builder.spotlighting()/canaryGuard()/injectionDefense()` 默认关。spec 07 新增「读侧注入防御」节。测试：`InjectionDefenseUnitTest`（5）、`InjectionDefenseEndToEndTest`（2：泄漏+变体拦截+写侧 dangerous 零调用；spotlight 保真）、`SpotlightSpillInteractionTest`（2）。Tier-2（向量拒识）留后续。
