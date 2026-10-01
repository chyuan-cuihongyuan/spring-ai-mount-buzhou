# Spec 10035 — X 系 X36 周期对账（effort #10035，X36）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10071–X10072，impl 2438）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 6 五新类型未入快照——三门批核：快照批补登 +5、api-surface 同步、CONTEXT 计数、README 行。

## Solution

快照 regenerate diff=+5 精确（1397 类型）；api-surface.md +5；CONTEXT 1392→1397；三门核绿+五组件测全绿；36/50=72% 里程碑。

## Testing Decisions

regenerate 再生 diff=+5 即精确（reactor classpath 口径）；组合定向 verify 五组件+三门 starter。

## Out of Scope

不做全仓 verify（R48/V48/W 环境豁免口径延续并入档）。

## Further Notes

Wave 6 开发勘误五处入档（2SAT 字面量 1 起址换面+PR discharge 护栏计数+KS 候选池跳键与哨兵勘误+DT 换静脉 CHK）——诚实工程档案。
