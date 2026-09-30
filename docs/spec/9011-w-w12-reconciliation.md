# Spec 9011 — W 系 W12 周期对账（effort #9011，W12）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9023–W9024，impl 2364）。
> 借鉴：V 系对账轮公式（spec 8023/8029/…）——周期台账纪律

## Problem Statement

十二轮积累的快照/覆盖/台账漂移静默风险——
周期对账。

## Solution

快照 regenerate diff=+5 精确；api-surface.md +5；
覆盖门+对账门+五组件 19 测组合定向 verify。

## Testing Decisions

三门+组件全绿即过；diff 非精确即红。

## Out of Scope

不做全仓 verify（R48/V48 环境豁免口径延续）；
不做 CONTEXT 计数同步（W50）。

## Further Notes

Wave 2 开发勘误三处入档（连通校验补位/替罪羊
取最高失衡祖先/编译失败静默教训——纪律强化：先见
全绿再 commit）。
