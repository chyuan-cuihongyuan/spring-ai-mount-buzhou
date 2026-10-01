# Spec 10047 — X 系 X48 周期对账（effort #10047，X48）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10095–X10096，impl 2450）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 8 五新类型未入快照——三门批核：快照批补登 +5、api-surface 同步、CONTEXT 计数、README 行。

## Solution

快照 regenerate diff=+5 精确（1407 类型）；api-surface.md +5；CONTEXT 1402→1407；三门核绿+五组件测全绿；48/50=96% 里程碑。

## Testing Decisions

regenerate 再生 diff=+5 即精确（reactor classpath 口径）；组合定向 verify 五组件+三门 starter。

## Out of Scope

不做全仓 verify（R48/V48/W 环境豁免口径延续并入档）。

## Further Notes

Wave 8 开发勘误四处入档（NB logUnseen 预存+感知机手算锚两次+CART 严格下降换不升即裂）——诚实工程档案。
