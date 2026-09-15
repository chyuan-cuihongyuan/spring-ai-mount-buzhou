# impl 1425 — StaleWhileRevalidatePolicy SWR 策略（R25 = effort #1824 / spec 1824 / T2849-T2850）

**What**：`StaleWhileRevalidatePolicy`（buzhou-resilience/cache 静态纯函数）——
serving 三态（FRESH/STALE 旧值+异步刷新/EXPIRED 同步回源，边界归属显式）+
staleness 读数（新鲜钳 0/窗内 (0,1)/过期 ≥1；fresh=0 -1 哨兵）；零窗退化
纯 TTL；负值 fail-fast。

**Why**：HTTP Cache-Control stale-while-revalidate/CDN 思想——TTL 二值逼出
「同步吃回源延迟 vs 放宽 TTL 拿旧还不自知」两难；陈旧窗中间态让调用方
5ms 拿旧值+后台刷新，p99 不吃回源。

**Verify**：`StaleWhileRevalidatePolicyTest` 4 用例全绿。

**Status**：done（2026-09-16）
