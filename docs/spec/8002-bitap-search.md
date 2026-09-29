# Spec 8002 — BitapSearch 位并行匹配（effort #8002，V3）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8005–V8006，impl 2304）。
> 借鉴：Baeza-Yates & Gonnet 1992（agrep/ripgrep 同源思想）。

## Problem Statement

短模式搜索的病：逐字符状态机分支密集——**模式匹配状态
本可压进一个机器字按位并行推进**；分支预测失败的常数
开销在短模式下被放大。

## Solution

`BitapSearch`（core/metrics，静态工具面）：Shift-And 位并行
——字符→位掩码表，状态 `state=(state<<1|1)&mask[c]` 单字
推进；命中=状态高位 1，`i−m+1` 即起始位；模式长 ≤63
（long 位宽上限诚实拒绝越界）；`findAll` 全部（含重叠）
命中；null/空模式 fail-fast；确定性纯函数。

## Testing Decisions

- 手锚（aabcfaab/aab→[0,5]；aaaa/aa 重叠 [0,1,2]；63 位边界
  恰好通过/64 拒绝）；300 随机 vs indexOf 圣像逐步全等；
  fail-fast（null/空/超位宽）。

## Out of Scope

- 不做 k-近似变体（插入/删除容忍）；不做流式增量。

## Further Notes

- 与 KmpSearch（3014）/BoyerMooreSearch（8001）同族不同面：
  失配函数 vs 双启发滑动 vs 位并行单字推进。
- 里程碑：V3/50（6%）。
