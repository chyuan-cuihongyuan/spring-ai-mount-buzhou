# impl 1319 — HookErrorDistribution 钩子异常类型分布（R20 = effort #1719 / spec 1719 / T2639-T2640）

**What**：「钩子名:异常简单类名」指纹分组+基数 32 超限并 _overflow_ 桶+census() 降序保序（unmodifiableMap）+total()。
**Why**：Sentry 事件分组——坏钩子排名显形（与 HookTimingAggregator 耗时面互补）。
**Verify**：HookErrorDistributionTest 4 断言全绿。 **Status**：done（2026-09-15）
