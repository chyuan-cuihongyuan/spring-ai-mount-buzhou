# Spec 3025 — Rabin-Karp 滚动哈希搜索（effort #3025，R26）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5051–T5052，impl 2026）。
> 借鉴：Karp-Rabin 1987（多项式滚动哈希）。

## Problem Statement

滑窗场景（流式敏感串/滑窗指纹/去重）每步全窗重哈希 O(m)——总
O(n·m)；需要 O(1) 滚动更新的哈希口径与可复核的命中语义。

## Solution

`RabinKarpSearch`（core/metrics，纯函数静态件）：

- 滚动递推：h' = (h − 左出字符·B^(m−1))·B + 右进字符——O(1)/步；
- 哈希命中**逐字复核**（Las Vegas——环绕假阳命中零误报）；
- `indexOf`/`findAll`（可重叠）与 KmpSearch 双约定对齐（空模式
  indexOf 0 / findAll 拒绝）；`hashOf` 内容哈希读数（同内容
  同哈希——滚动与整串同口径）；long 溢出环绕即隐式取模（Java
  环绕确定性，免显式 mod 负数陷阱）。

## User Stories

1. 作为流式守卫作者，滑窗指纹 O(1) 滚动更新——步进免重哈希。
2. 作为对账作者，hashOf 锚点可复算——哈希口径可审计。

## Testing Decisions

- 300 随机对拍三方一致（JDK indexOf + KmpSearch + 本件——
  三字母表高重叠压力）；可重叠 findAll（[0,1,2] 与 [0,2]）；
  无匹配 −1/空；空模式双约定；内容哈希位置无关性+嵌入宿主命中；
  千级高重复文本滚动压力（唯一命中尾窗+1998 重叠命中——环绕
  不产生假阳）。

## Out of Scope

- 不做多模式集（哈希桶聚合留白）；不做双哈希降碰撞（单哈希+
  复核已零误报）；不做字节级（char 口径）。

## Further Notes

- 与 KmpSearch 成对：KMP 失配跳跃（单模式确定）、本件滚动指纹
  （口径可复用）——各取所长。
- 里程碑：26/150。
