# Spec 152 — 心跳钩子（effort #121）

> wayfinder map：`.wayfinder/maps/effort-121.md`（T505–T506）。spec 138（TurnHeartbeat
> 注册表）的接线面。

## Problem Statement

TurnHeartbeat 落了表但没人打点：宿主要自己在各挂点手工调 beat——接线面缺失
让「活着但不动」检测停留在库能力。

## Solution

`runaway/TurnHeartbeatHook`：beforeTurn 注册 / afterTurn 清除（轮次起止对齐
在飞表）；beforeModel/afterModel/beforeTool/afterTool 四点自动 beat。order 50
（裁决类钩子之前——先留痕后裁决：后续钩子 block/替换也有「到过这里」的进展
信号）；全挂点 CONTINUE（观测面永不干预）。`heartbeat()` 共享视图供宿主巡检
stalled。

## User Stories

1. 作为宿主开发者，RuntimeConfig 挂一个 hook 即得心跳事实表，所以停滞巡检
   零手工接线。
2. 作为 SRE，模型悬挂期间 lastBeat 持续可见，所以 stalled 查询能区分「模型
   慢」与「真卡死」。

## Testing Decisions

- e2e：悬挂模型（latch 释放）下断言 inFlight=1 + lastBeat 非空 + 不判停滞 +
  结束后清除归零；null 心跳自持实例不 NPE。全量 BUILD SUCCESS（含 #119/#120
  随行修复回归）。

## Out of Scope

- 定期自动巡检；软取消；持久化。

## Further Notes

- 观测钩子族先例：只打点不裁决（与 token 累计 hook 的 afterModel 同纪律）。
