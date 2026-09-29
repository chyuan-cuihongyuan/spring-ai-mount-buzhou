# Spec 8014 — MinWindowSubstring 最小覆盖子串（effort #8014，V15）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8029–V8030，impl 2316）。
> 借鉴：滑动窗口经典（CLRS/LeetCode 最小覆盖子串——需求计数差分面）。

## Problem Statement

覆盖匹配的病：全起点全终点枚举 O(n²m)——**需求计数 +
双指针伸缩把右界推进均摊 O(n)**，窗口合法性 O(1) 维护。

## Solution

`MinWindowSubstring`（core/metrics，静态工具面）：need 计
数表 + window 计数表 + satisfied 双指针；右界扩张补满足、
左界收缩去冗余，记录最短；并列取最左（canonical——同输入
同结果）；无覆盖返回空串（诚实缺省）；null/空模式 fail-fast；
确定性纯函数。

## Testing Decisions

- 经典手锚（ADOBECODEBANC/ABC→BANC；a/a→a；a/aa→""；
  并列取最左）；300 随机 vs 暴力 O(n²m) 圣像（长度+内容
  全等）；fail-fast。

## Out of Scope

- 不做流式增量；不做字符集外的通配语义。

## Further Notes

- 与 BoyerMooreSearch（8001）同族不同面：精确子串定位 vs
  多重需求覆盖窗口。
- 勘误入档：原拟静脉 QuotientFilter 删除路径簇边界修复超
  时预算退回雾区（U24 同款纪律），备选池补位——V18 对账轮
  快照口径同步。
- 里程碑：V15/50（30%）。
