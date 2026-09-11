---
id: T858
title: 事实置信度衰减的形态裁决（读时半衰 vs 写回降权）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

letta memory blocks 给事实带置信度并随时间衰减（陈年低置信不再注入）。本仓 Fact 模型只有轮次 TTL——衰减应取什么形态？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 5 轮 = effort #600 / spec 604 / impl 457）：

1. **读时衰减不写回**：`DecayingFactStore` 装饰器在 activeFacts 上按 `conf × 2^(−elapsed/halfLife) ≥ floor` 过滤——幂等可逆，换策略立即生效；写回会破坏可逆性且要处理并发。
2. `Fact` 扩第 6 组件 `confidence ∈ (0,1]`（五参兼容构造 = 1.0）；信封携带，旧信封兼容读 1.0。
3. 冲突驱动的置信下调（新矛盾事实压低旧事实）留雾区——需要事实更新事件流，本轮不做。
4. 模块边界：装饰器与策略放 buzhou-memory `memory.facts`（不引 core internal）；信封往返测试留在 core 自己的 DefaultFactStoreTest。
