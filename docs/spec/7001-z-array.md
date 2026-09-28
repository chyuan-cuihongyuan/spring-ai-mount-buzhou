# Spec 7001 — Z Array Z 数组（effort #7001，U2）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7203–U7204，impl 2253）。
> 借鉴：Gusfield Z 算法（竞争编程/字符串索引同源）。

## Problem Statement

前缀匹配的病：逐位置暴力 LCP O(n²)（长串放大）——
**Z-box 匹配段区间复用的线性前缀面**缺失。

## Solution

`ZArray`（core/metrics，静态工具面同 KmpSearch）：

- z[i] = 后缀 s[i:] 与整串的最长公共前缀长；Z-box [l,r)
  匹配段复用摊还 O(n)；z[0]=n（整串自比约定）；
- 模式匹配经 p+SEP+t 一次 z 扫描得全部（重叠）命中；
  SEP 用 NUL——任一侧含 NUL fail-fast（分隔符唯一性前提）；
- zFunction/findAll/count 读数；null/空串 fail-fast。

## User Stories

1. 作为检索作者，线性预处理后任意模式一次扫描全命中。
2. 作为审计作者，随机串 vs 暴力 LCP 圣像逐步全等。

## Testing Decisions

- 手锚三例（aaaa/abacaba/aabxaab）逐值钉住；200 随机串
  vs 暴力 LCP；findAll vs startsWith 圣像；重叠命中；fail-fast。

## Out of Scope

- 不做最小表示/扩展 KMP 面外语义。

## Further Notes

- 与 KmpSearch（3014）同族不同面：失败函数边界回归 vs
  Z-box 区间复用。
- 里程碑：U2/50（4%）。
