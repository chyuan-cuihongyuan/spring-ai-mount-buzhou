# Spec 10005 — X 系 X6 周期对账（effort #10005，X6）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10011–X10012，impl 2408）。
> 借鉴：W 系 W6k 对账公式（spec 9005 族）——周期对账纪律

## Problem Statement

Wave 1 四新类型未入快照——覆盖/快照/
对账三门需批核：快照批补登 +4、api-surface
同步、CONTEXT 计数、README 行。

## Solution

快照 regenerate diff=+4 精确（1372 类型）；
api-surface.md +4；CONTEXT 1368→1372；
三门核绿（覆盖 2/2+快照门+对账门）；
四组件 21 测全绿；6/50=12% 里程碑。

## Testing Decisions

regenerate 再生 diff=+4 即精确（reactor
classpath 口径）；组合定向 verify 四组件
定向测 + 三门 starter 定向测。

## Out of Scope

不做全仓 verify（R48/V48/W 环境豁免口径
延续并入档）；不做 push（GitHub 离线——
对账轮重试口）。

## Further Notes

Wave 1 占坑勘误三换（EliasFano 词根漏检
→InterpolativeCoding 等）已随轮入档；
X3 两处结构勘误与 X4 密度勘误入档。
