---
id: T1808
title: PolicyGateHook 指标注释 tag 值失真（allowed|blocked|escalated → allow|deny|escalate）
type: task
status: closed
assignee: zcode-k
blocked-by:
  - T1806
created: 2026-09-15
---

## Question

R2 补测显形：PolicyGateHook 源注释称指标 `buzhou.guard.checks` 的 `outcome=allowed|blocked|escalated`，而代码发射 `decision.action().name().toLowerCase()` = `allow|deny|escalate`。哪边是合同？

## Resolution

**用户常设授权 AFK（可推翻）**

裁决（K 会话第 2 轮 = effort #1201）：**实际合同 = Action 名小写**（`allow|deny|escalate`），注释过期失真——修正注释而非改代码：

1. `docs/spec/13-production-hardening.md` §T66 原文未定义 tag 值——「allowed|blocked|escalated」只存在于 PolicyGateHook 行内注释，无规格背书。
2. 代码行为自洽且与其他指标同风格（tag 值 = 枚举名小写，如 HookResult 场景惯例）；`allow|deny|escalate` 与 `PolicyDecision.Action` 枚举一一对应，映射零成本、可 grep。
3. 改代码（改成 allowed|blocked|escalated）是**指标 tag 值行为变更**——会破坏任何按现有值做看板/告警的部署侧，须独立 spec 决策而非测试轮夹带。
4. 修复：注释更正为 `outcome=allow|deny|escalate`（零行为变化），测试以实际值锁合同（PolicyGateHookTest#metricsCounterTaggedByOutcome）。
