# impl 1452 — BucketTableSizing 桶表容量阶梯（R52 = effort #1851 / spec 1851 / T2903-T2904）

**What**：`BucketTableSizing`（core/cache 静态纯函数）——suggestCapacity
（⌈n/lf⌉ 向上 2 的幂）+ verdict（装填度≥lf 边界含）+ load 读数；默认
lf 0.75；畸形五型 fail-fast。

**Why**：HashMap 负载因子+2 的幂容量惯例——自建桶表初容量/扩容时机/
装填前瞻三件套基建；碰撞链超线性拐点前动手。

**Verify**：`BucketTableSizingTest` 4 用例全绿（首跑红为测试期望算术误
修正——本系第三次测试数据病理，模式已识别入档）。

**Status**：done（2026-09-16）
