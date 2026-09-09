# Wayfinder Map — Buzhou 心跳钩子接线（effort #121，A 会话第 16 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 138 的接线面（同 spec150 模式）。

## Destination

TurnHeartbeatHook：beforeTurn 注册 / afterTurn 清除 / 模型与工具四点自动打点
——宿主零手工调用即得停滞检测事实表。

## Notes

- order 50（裁决类钩子之前——先留痕后裁决，后续 block 也留「到过这里」）；
  永续 CONTINUE（观测面不裁决）；随行修复 #119/#120 提交后暴露的编译错
  （block 文案引号）与门禁测试触发面（store 层已拒空行——改重复行+自定义
  期望），本轮全量 BUILD SUCCESS 验证。

## Decisions so far

- [TurnHeartbeatHook](../tickets/T505-heartbeat-hook.md) — 六挂点全接 +
  heartbeat() 共享视图。

## Not yet specified

- 停滞自动巡检（定期 stalled 查询 + 告警事件）；与 TurnDeadline 组合的软取消。

## Out of scope

- 持久化；分布式汇聚。

## Tickets

- [x] [T505 心跳钩子](../tickets/T505-heartbeat-hook.md)（impl-288）
- [x] [T506 收口提交](../tickets/T506-heartbeat-hook-close.md)（impl-288）
