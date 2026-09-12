---
id: T879
title: GCRA 装配验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T878
created: 2026-09-12
---

## Question**

装配语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（GcraAssemblyTest 3/3 + BuzhouResilienceAutoConfigurationTest 11/11 含新 yml 绑定用例；resilience 全模块 237/237）：

- 默认/显式 token-bucket 走令牌桶口径；smoothing 未知值装配期 fail-fast。
- gcra 声明 + FAIL_FAST + 宽裕配额下 chat 端到端跑通。
- yml：smoothing=gcra + gcra-burst-tolerance=5s 正确绑定（多构造 @ConstructorBinding）。
