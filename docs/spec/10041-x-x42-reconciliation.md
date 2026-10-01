# Spec 10041 — X 系 X42 周期对账（effort #10041，X42）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10083–X10084，impl 2444）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 7 五新类型未入快照——三门批核：快照批补登 +5、api-surface 同步、CONTEXT 计数、README 行。

## Solution

快照 regenerate diff=+5 精确（1402 类型）；api-surface.md +5；CONTEXT 1397→1402；三门核绿+五组件测全绿；42/50=84% 里程碑。

## Testing Decisions

regenerate 再生 diff=+5 即精确（reactor classpath 口径）；组合定向 verify 五组件+三门 starter。

## Out of Scope

不做全仓 verify（R48/V48/W 环境豁免口径延续并入档）。

## Further Notes

Wave 7 开发勘误两处入档（LombScargle 白噪极值口径+FFT 双数组长度不配消息面）——诚实工程档案。
