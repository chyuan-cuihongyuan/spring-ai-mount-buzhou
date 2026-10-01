# Spec 11017 — Y 系 Y18 周期对账（effort #11017，Y18）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11035–Y11036，impl 2470）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 3 五新类型未入快照——三门批核：快照批补登 +5、api-surface 同步、CONTEXT 计数、README 行。

## Solution

快照 regenerate diff=+5 精确（1422 类型）；api-surface.md +5；CONTEXT 1417→1422；三门核绿+五组件测全绿；18/50=36% 里程碑。

## Testing Decisions

regenerate 再生 diff=+5 即精确（reactor classpath 口径）；组合定向 verify 五组件+三门 starter（X/Y 双对账门）。

## Out of Scope

不做全仓 verify（R48 环境豁免口径延续并入档）。

## Further Notes

Wave 3 开发勘误四处入档（YW ρ/r0 域一致性+水塘断言类型+RANSAC 圣像锚+Platt 域复原）——诚实工程档案。
