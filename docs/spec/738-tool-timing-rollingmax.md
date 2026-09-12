# 738 — 工具侧滚动 max 同构扩散

> 来源：G 会话第 38 轮 = effort #738（spec 708 工具侧同构扩散）/ [T1025](../../.wayfinder/tickets/T1025-tool-timing-rollingmax-shape.md) / [T1026](../../.wayfinder/tickets/T1026-tool-timing-rollingmax-verify.md) / impl 540。

## 背景

HookTiming 已有滚动 max（spec 708）——工具侧 ToolTimingAggregator 生命周期 max 同样永不衰减，同构扩散。

## 目标

- ToolTimingAggregator.Timing 增 windowedMax（RollingMaxCounter 复用）；`windowedMax()` 快照；ToolTimingHealth 行增 `rollingMaxMicros`。

## 测试

record 后 windowedMax 非零、健康行含 rollingMaxMicros；既有用例零回归。

## 兼容性

additive；stats() 口径零变化。
