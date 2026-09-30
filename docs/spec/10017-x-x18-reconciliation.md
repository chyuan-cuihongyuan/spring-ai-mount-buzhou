# Spec 10017 — X 系 X18 周期对账（effort #10017，X18）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10035–X10036，impl 2420）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 3 五新类型未入快照——三门批核：
快照批补登 +5、api-surface 同步（concurrent
1 行+policy 4 行）、CONTEXT 计数、README 行。

## Solution

快照 regenerate diff=+5 精确（1382 类型）；
api-surface.md +5；CONTEXT 1377→1382；
三门核绿+五组件测全绿；18/50=36% 里程碑。

## Testing Decisions

regenerate 再生 diff=+5 即精确（reactor
classpath 口径）；组合定向 verify 五组件
+三门 starter。

## Out of Scope

不做全仓 verify（R48/V48/W 环境豁免口径
延续并入档）。

## Further Notes

Wave 3 开发勘误五处入档（DT 有向边界/
超三角格点两处+SH 角点双发+CR 共线锚
端段+MS double[] 身份比较）——诚实工
程档案。
