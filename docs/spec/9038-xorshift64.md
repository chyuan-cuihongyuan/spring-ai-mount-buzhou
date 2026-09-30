# Spec 9038 — XorShift64 伪随机数（effort #9038，W39）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9077–W9078，impl 2391）。
> 借鉴：xorshift（Marsaglia 2003「Xorshift RNGs」——13,7,17 三移位模板）

## Problem Statement

LCG 低位短周期、LFSR 单比特输出——
**xorshift64**：三移位异或，无乘法
极简 64 位全宽随机流。

## Solution

XorShift64（core/metrics，静态纯函数面）：
next(state) 推进即值；nextLong(state,bound)
拒绝采样无偏；种子 0 拒绝。

## Testing Decisions

同种子 1000 步全等恒非零；10 万步互异+
高位铺展；bound=100 分布圣像±20%；
fail-fast。

## Out of Scope

不做 xorshift*+/plus 变体（另立面）；
不做密码学承诺（统计面明示）；不做流封装。

## Further Notes

与 SplitMix64（9039）同族不同面：
推进器 vs 定序器。Wave 7 第三件。
