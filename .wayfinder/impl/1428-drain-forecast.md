# impl 1428 — DrainForecast 排空预测（R28 = effort #1827 / spec 1827 / T2855-T2856）

**What**：`DrainForecast`（core/session 静态纯函数）——forecast → makespan
（max(最大单会话, ceil(总÷并行))×单位耗时）+ bottleneckSession 瓶颈直读 +
parallelismBound 主导方（相等取单会话）；并行度<1/负耗时/空白 id/负剩余
fail-fast。

**Why**：k8s drain/Envoy shutdown drain 思想——停机超时来自排空负载的
makespan 预测：workBound 催单点、parallelismBound 加并行或延窗，超时 =
预测×安全余量而非魔法数。

**Verify**：`DrainForecastTest` 5 用例全绿。

**Status**：done（2026-09-16）
