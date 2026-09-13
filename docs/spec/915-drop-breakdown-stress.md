# 915 — EventDropBreakdown 并发压测

> 来源：I 会话第 16 轮 = effort #915（[T1281](../../.wayfinder/tickets/T1281-drop-breakdown-stress-shape.md) / [T1282](../../.wayfinder/tickets/T1282-drop-breakdown-stress-verify.md) / impl 668）。G 会话 r47 压测轮同模式（ToolTimingAggregatorConcurrencyTest 先例）。

## 背景

spec 900 的丢弃分类计数是「总量 AtomicLong.incrementAndGet + 分类 CHM.computeIfAbsent + LongAdder.increment」两步复合写——**非原子**。enqueue 多调用方并发（会话多线程事件源）时守恒不变量（ΣbyReason == dropped）依赖每步的并发正确性。单测只覆盖串行。

## 目标

`EventDropBreakdownConcurrencyTest`（core.internal.session 测试域）：

- parallel stream 4000 并发 enqueue（容量 2 + 慢消费闩 → 恒 DROP_OLDEST 溢出）；
- 断言：
  1. **守恒**：`dropBreakdown().total() == stats().dropped()`（压测核心不变量）；
  2. 不丢计数：`dropped + delivered == enqueued`（预算口径近似——close 后断言）；
  3. 分类值域封闭：`byReason().keySet() ⊆ {drop-oldest, drop-oldest-race, closed-undelivered}`；
  4. 多分发器实例互不串账（3 实例各 500 并发，各自守恒独立成立）。
- 压测本身即热路径验证（全程秒级——CHM+LongAdder 无锁路径）。

## 兼容性

纯测试轮；零生产代码变更。
