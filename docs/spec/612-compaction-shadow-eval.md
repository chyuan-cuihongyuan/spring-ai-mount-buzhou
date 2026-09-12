# 612 — 微压缩影子干跑评估

> 借鉴：[istio/istio](https://github.com/istio/istio) mirroring——影子路径评估不生效。
> 来源：F 会话第 13 轮 = effort #600 / [T874](../../.wayfinder/tickets/T874-compaction-shadow-shape.md) / [T875](../../.wayfinder/tickets/T875-compaction-shadow-verify.md) / impl 465。

## 背景

微压缩策略（evictRatio / maxAgeTurns / minSizeChars）调参只能「真压」才见效果；对生产会话调参有行为风险。微压缩是确定性纯函数（不调 LLM）——干跑零成本。

## 目标

`CompactionShadowEvaluator`：对真实历史跑 compact 但不应用——报告会压的 id 与可回收字符；梯度 sweep 出调参表；事件外发（applied=false 显式）。

## 非目标

- LLM 摘要压缩的影子（额外摘要调用有真实成本）——雾区。
- 不写 store、不 mutate 历史（零变异钉住）。
- 不接自动调参（表供人/上层决策）。

## 设计

- `evaluate(history, turn, policy, protect, ratio)` → `ShadowReport(ratio, wouldCompactIds, reclaimedChars)`。
- `sweep`：0.25/0.5/0.75/1.0（Letta ~70% 逐出语境）。
- `evaluateAndEmit`：`memory.compaction-shadow` 事件（payload 含 applied=false）。

## 测试

4 用例：与直压同口径+零变异 / sweep 单调 / 事件形状 / 构造校验。

## 兼容性

纯增量新类；不改压缩执行面。
