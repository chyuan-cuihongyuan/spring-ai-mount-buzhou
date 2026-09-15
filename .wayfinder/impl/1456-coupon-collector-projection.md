# impl 1456 — CouponCollectorProjection 收藏家覆盖期望（R56 = effort #1855 / spec 1855 / T2911-T2912）

**What**：`CouponCollectorProjection`（core/eval 静态纯函数）——
expectedDraws(k)=k×H(k) + expectedRemaining(s,k)=k×(H(k)−H(s))；double
防溢出；0≤seen≤k 契约 fail-fast。

**Why**：概率论 coupon collector 思想——均匀抽样见全 k 类期望 k·H(k)
（长尾在最后一类）；覆盖测试轮数预算从拍到有期望依据，「还差几轮收齐」
随进度可读。

**Verify**：`CouponCollectorProjectionTest` 4 用例全绿。

**Status**：done（2026-09-16）
