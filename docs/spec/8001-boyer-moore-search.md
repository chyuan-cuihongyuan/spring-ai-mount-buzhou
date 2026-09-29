# Spec 8001 — BoyerMooreSearch 坏字符/好后缀搜索（effort #8001，V2）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8003–V8004，impl 2303）。
> 借鉴：Boyer & Moore 1977（grep/less 同源思想）。

## Problem Statement

子串搜索的病：KMP 失配只按模式自知识滑动——**文本侧信息
（坏字符）弃而不用**，实际文本常比模式提供的滑动量更慷慨；
朴素逐位 O(nm) 更不可承受。

## Solution

`BoyerMooreSearch`（core/metrics，静态工具面）：坏字符表
（模式内最右出现位）+ 强好后缀表（后缀边界的经典 shift 构造）
双启发取大滑动 O(n/m) 期望；`findAll` 返回全部（含重叠）命中；
模式空/null fail-fast；确定性纯函数（同输入同命中序）。

## Testing Decisions

- 手锚（aabcfaab/aab→[0,5]；aaaa/aa 重叠 [0,1,2]；abababab/abab
  →[0,2,4]；同串 [0]；无命中/长模式空表）；300 随机 vs
  indexOf 圣像逐步全等；fail-fast。

## Out of Scope

- 不做正则/通配；不做流式增量（整串面）。

## Further Notes

- 与 KmpSearch（3014）/RabinKarpSearch（3025）同族不同面：
  失配函数自知识 vs 滚动哈希 vs 文本侧坏字符+好后缀双启发。
- 里程碑：V2/50（4%）。
