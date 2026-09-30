# Spec 9005 — W 系 W6 周期对账（effort #9005，W6）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9011–W9012，impl 2358）。
> 借鉴：V 系对账轮公式（spec 8023/8029/…）——周期台账纪律

## Problem Statement

六轮积累的快照/覆盖/台账漂移静默风险——周期对账：
快照批补登+三门核绿+台账核账。

## Solution

快照 regenerate 再生（-am reactor 口径）diff 核对
=+4 精确；api-surface.md 同步 +4 行；SpecCoverage/
WSession9000LedgerAudit/四组件测试组合定向 verify。

## Testing Decisions

三门+组件 16 测全绿即过；diff 非精确（多删少补）
即红。

## Out of Scope

不做全仓 verify（R48/V48 环境豁免口径延续——
满载偶红重跑协议）；不做 CONTEXT 计数同步（W50 收口轮）。

## Further Notes

W2 轮 README 锚残行死链勘误入档（w_round.py 号段
公式 bug 的下游——公式已修+残行清除）。
