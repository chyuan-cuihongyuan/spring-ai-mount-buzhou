# Spec 9023 — W 系 W24 周期对账（effort #9023，W24）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9047–W9048，impl 2376）。
> 借鉴：V 系对账轮公式（spec 8023/8029/…）——周期台账纪律

## Problem Statement

二十四轮积累的快照/覆盖/台账漂移静默风险——
周期对账。

## Solution

快照 regenerate diff=+5 精确；api-surface.md +5；
覆盖门+对账门+五组件 16 测组合定向 verify。

## Testing Decisions

三门+组件全绿即过；diff 非精确即红。

## Out of Scope

不做全仓 verify（R48/V48 环境豁免口径延续）；
不做 CONTEXT 计数同步（W50）。

## Further Notes

Wave 4 勘误入档三处（占坑换 TfIdf+
null 断言载体+随机区间+Morse E 辨析）。
