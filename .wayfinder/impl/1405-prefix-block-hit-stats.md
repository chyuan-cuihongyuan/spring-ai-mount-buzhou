# impl 1405 — PrefixBlockHitStats 前缀块命中读面（R5 = effort #1804 / spec 1804 / T2809-T2810）

**What**：`PrefixBlockHitStats`（buzhou-resilience/cache 静态纯函数）——
`analyze(blockSize, prompts)` 等长块切分命中账：尾块不入账（vLLM partial
block）、同请求内重复块只记首见（radix 一次插入）、跨请求再见计复用；
BlockReport 带 blockHitRatio/blockMissRatio（零入账 -1 哨兵）、
DEFAULT_BLOCK_SIZE=64 常量。

**Why**：vLLM block-level prefix cache / SGLang radix tree 思想——整请求
命中率贴地但块命中率高 = 前缀底子厚该投 radix 索引/预热；块命中率也贴地 =
前缀投资无回报。两读数分开，缓存投资才有依据。

**Verify**：`PrefixBlockHitStatsTest` 6 用例全绿（含无状态性回归——初版
曾误用静态索引，纯函数性用例钉死防复发；首跑 1 红为测试数据与去重语义
交互，数据修正后转绿）。

**Status**：done（2026-09-16）
