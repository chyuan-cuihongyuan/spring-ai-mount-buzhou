# Spec 11023 — Y 系 Y24 周期对账（effort #11023，Y24）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11047–Y11048，impl 2476）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 4 五新类型未入快照——三门批核：快照批补登 +5、api-surface 同步、CONTEXT 计数、README 行。

## Solution

快照 regenerate diff=+5 精确（1427 类型）；api-surface.md +5；CONTEXT 1422→1427；三门核绿+五组件测全绿；24/50=48% 里程碑。

## Testing Decisions

regenerate 再生 diff=+5 即精确（reactor classpath 口径）；组合定向 verify 五组件+三门 starter（X/Y 双对账门）。

## Out of Scope

不做全仓 verify（R48 环境豁免口径延续并入档）。

## Further Notes

Wave 4 开发勘误六处入档（BCC 回边单压+Bridge int[] 身份+GomoryHu 换静脉 EG+HH 神像二次+Karger 换静脉 MCO+SlopeOne 直读与共评者）——诚实工程档案。
