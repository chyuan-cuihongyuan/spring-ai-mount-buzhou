# Spec 10029 — X 系 X30 周期对账（effort #10029，X30）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10059–X10060，impl 2432）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 5 五新类型未入快照——三门批核：
快照批补登 +5、api-surface 同步、CONTEXT
计数、README 行。

## Solution

快照 regenerate diff=+5 精确（1392 类型）；
api-surface.md +5；CONTEXT 1387→1392；
三门核绿+五组件测全绿；30/50=60%
里程碑。

## Testing Decisions

regenerate 再生 diff=+5 即精确（reactor
classpath 口径）；组合定向 verify 五组件
+三门 starter。

## Out of Scope

不做全仓 verify（R48/V48/W 环境豁免口径
延续并入档）。

## Further Notes

Wave 5 开发勘误三处入档（Sobol 无符号
掩码口径+Halton 逆根手锚勘误+PCG 种子
预热口径）——诚实工程档案。
