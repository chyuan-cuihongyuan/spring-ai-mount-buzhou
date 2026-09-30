# Spec 9041 — W 系 W42 周期对账（effort #9041，W42）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9083–W9084，impl 2394）。
> 借鉴：V 系对账轮公式（spec 8023/8029/…）——周期台账纪律

## Problem Statement

四十二轮积累的快照/覆盖/台账漂移静默风险——
周期对账。

## Solution

快照 regenerate diff=+5 精确；api-surface.md +5；
覆盖门+对账门+五组件 15 测组合定向 verify。

## Testing Decisions

三门+组件全绿即过；diff 非精确即红。

## Out of Scope

不做全仓 verify（R48/V48 环境豁免口径延续）；
不做 CONTEXT 计数同步（W50）。

## Further Notes

Wave 7 勘误入档四处（HOTP 占坑/RFC 常量
证伪/金向量 Python 锁定/比较器数同构递归）。
