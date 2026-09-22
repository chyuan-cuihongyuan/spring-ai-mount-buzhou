# impl 1485 — SlidingWindowCounter 滑动窗口计数器（R85 = effort #1884 / spec 1884 / T2969-T2970）

**What**：`SlidingWindowCounter`（core/ratelimit 静态纯函数）——
estimate（prev×(1−ratio)+curr 流逝比插值）+ wouldExceed（估计 ≥
限值满额拒绝）；计数≥0/ratio∈[0,1]/limit≥0 fail-fast。

**Why**：Cloudflare sliding window counter——固定窗边界可突发 2×、
滑窗日志逐请求记账内存大；两固定窗计数插值拿近似滑窗速率，零状态
可叠在既有固定窗设施上。

**Verify**：`SlidingWindowCounterTest` 4 用例全绿（插值四例/准入
边界两例/零流量/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
