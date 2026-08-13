---
id: T16
title: core · 工具错误回喂模型（Turn 不死于单工具异常）
type: task
status: closed
assignee: "chyuan"
blocked-by: []
epic: T12
created: 2026-08-13
---

**Status:** ready-for-agent · **Spec:** [docs/spec/11](../../docs/spec/11-best-of-breed-adoption.md#core)

## What to build

当工具执行抛异常或工具缺失时，执行脊柱不再上抛终结整轮 Turn；而是合成一条结构化 ToolResponseMessage（含错误文案与原工具入参），按 tool_call 原序回注模型，让模型自我纠错、继续完成请求。与既有「单工具超时失败转文本」统一为同一「错误即反馈」通道。**仅作用于工具侧异常**，与模型韧性层（onModelError）正交、互不吞没。

## Acceptance criteria

- [ ] 端到端：一个工具抛异常时，Turn 不死、模型收到错误反馈并继续，最终完成用户请求
- [ ] 工具缺失场景同样回喂为错误结果而非崩溃
- [ ] 既有工具超时/取消/并行语义不回归（`HarnessToolCallingManagerTest` 全绿）
- [ ] 与模型侧异常处理（onModelError）边界清晰、互不干扰

## Blocked by

无 —— 可立即开工。（与 T17 同在执行脊柱，建议串行以免冲突。）

## Resolution

已落地（Tier-1）。工具侧全部失败路径（执行异常/超时/取消/中断/工具缺失）统一经 `ToolErrorFeedback`（buzhou-core/exec）合成结构化错误反馈（`[工具执行失败]` + 工具名 + **原入参** + 原因 + 纠错建议），按 tool_call 原序回注 `ToolResponseMessage`，Turn 不死；聚合层兜底保证每 tool_call 恒一个响应（含漏网 `Error`）。顺带修复零工具注册时 `getToolCallbacks()` NPE。与模型侧异常正交（e2e 显式守护：模型侧异常照常上抛）。spec 05「失败、超时与取消」同步修订。测试：`ToolErrorFeedbackTest`（6）、`HarnessToolCallingManagerTest`（5，既有断言语义保留）、examples `ToolErrorFeedbackIntegrationTest`（3：异常回喂自纠 / 缺失工具回喂 / 模型侧正交）。
