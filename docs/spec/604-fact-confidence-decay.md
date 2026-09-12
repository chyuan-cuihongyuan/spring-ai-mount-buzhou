# 604 — 事实置信度衰减

> 借鉴：[letta-ai/letta](https://github.com/letta-ai/letta)（MemGPT）memory blocks 置信度衰减——陈年低置信记忆停止注入。
> 来源：F 会话第 5 轮 = effort #600 / [T858](../../.wayfinder/tickets/T858-fact-decay-shape.md) / [T859](../../.wayfinder/tickets/T859-fact-decay-verify.md) / impl 457。

## 背景

Fact 只有轮次 TTL（硬过期）；「采集器不太确定的事实」与「确信事实」同权重注入到轮次用尽——陈年低置信事实长期占用提示词预算。letta 给记忆条目配置信度并随时间衰减。

## 目标

1. `Fact` 增 `confidence ∈ (0,1]`（默认 1.0，五参构造兼容；信封携带、旧信封读 1.0）。
2. `DecayingFactStore`（buzhou-memory，opt-in 装饰器）：`activeFacts` 在 TTL 过滤之上按 `conf × 2^(−elapsedTurns/halfLifeTurns) ≥ floor` 过滤。
3. `FactDecayPolicy(halfLifeTurns, floor)` 策略 record（defaults：半衰 8 / 下限 0.25）。

## 非目标

- 不做冲突驱动的置信下调（需事实更新事件流——雾区）。
- 不写回：衰减只影响注入读（幂等可逆，换策略立即生效）。
- 不改默认装配（未包装装饰器 = confidence 仅随信封往返，零行为变化）。

## 测试

DecayingFactStoreTest 5 用例（半衰边界/公式/直通/校验）+ DefaultFactStoreTest 信封兼容往返（模块边界：memory 侧用内联 FakeFactStore，不引 core internal）。

## 兼容性

Fact record 扩组件 + 五参兼容构造；信封向后兼容（旧格式读 1.0）。
