# Wayfinder Map — Buzhou 轮次心跳（effort #114，A 会话第 9 轮）

> A 侧编号策略沿用 #111 声明。原拟 DLQ/限流/退避抖动题均已被既有轮覆盖
> （spec54/spec24 族），本题换轮次心跳。借鉴 Temporal Activity heartbeat。

## Destination

在飞轮次的进展活性可见：hook 打点「最近动静」，停滞检测识别「活着但不动」
的轮次——deadline 未到、计数不涨、只有心跳能看见的卡死面。

## Notes

- 与 TurnDeadline（绝对时限）/ RunawayHook（行为失控计数）正交：心跳管进展
  活性；注册制（register/clear 对齐轮次起止——表只在飞轮次，天然有界）；
  stalled 只检候选不枚举全表（不猜生命周期）。

## Decisions so far

- [TurnHeartbeat](tickets/T463-turn-heartbeat.md) — beat/register/clear/stalled
  （quiet 降序——最长停滞优先）+ stalled-detected 计数 + 1024 封顶 fail-fast。

## Not yet specified

- Hook 接线（模型/工具/事件点自动打点）；停滞自动处置（告警/软取消）。

## Out of scope

- 持久化心跳（跨重启）；分布式心跳汇聚（B 侧舱表族正交）。

## Tickets

- [x] [T463 轮次心跳](tickets/T463-turn-heartbeat.md)（impl-281）
- [x] [T464 收口提交](tickets/T464-turn-heartbeat-close.md)（impl-281）
