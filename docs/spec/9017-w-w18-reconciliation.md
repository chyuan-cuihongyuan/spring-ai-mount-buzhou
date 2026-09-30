# Spec 9017 — W 系 W18 周期对账（effort #9017，W18）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9035–W9036，impl 2370）。
> 借鉴：V 系对账轮公式（spec 8023/8029/…）——周期台账纪律

## Problem Statement

十八轮积累的快照/覆盖/台账漂移静默风险——
周期对账。

## Solution

快照 regenerate diff=+5 精确；api-surface.md +5；
覆盖门+对账门+五组件 18 测组合定向 verify。

## Testing Decisions

三门+组件全绿即过；diff 非精确即红。

## Out of Scope

不做全仓 verify（R48/V48 环境豁免口径延续）；
不做 CONTEXT 计数同步（W50）。

## Further Notes

Wave 3 勘误入档：ZigZag 占坑换 Lz77+
AnsCodec 归一化负频率 OOM 根因+配对推导。
