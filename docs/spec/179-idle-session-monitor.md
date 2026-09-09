# Spec 179 — 空闲会话水位监控（effort #208）

> wayfinder map：`.wayfinder/maps/effort-208.md`（T549–T550）。借鉴：Flink watermark
> ——时间水位推进判定「谁掉队了」；空闲会话是资源水位里的掉队者。

## Problem Statement

会话空闲数小时后仍占驻留资源（特征表 LRU 逐出是被动兜底），压缩/归档想以
「空闲」为判据却没有统一供给面——每个机制自己记 lastSeen、自己判阈值，
口径不一且重复。特征仓（spec 161）已有 lastActiveAt 事实，缺一个消费它的
水位判定器。

## Solution

`IdleSessionMonitor`（core/session）：

- **判定**：`sweep(Instant now)`——特征仓快照中 lastActiveAt 距 now ≥ 阈值
  （默认 30min，可配）的会话即空闲；返回 `IdleInfo(sessionId, idleMillis)`
  清单（空闲时长降序——最该处理的排最前）。
- **翻转通知**：`onChange`——会话进入/离开空闲态才回调（不刷屏）；离开 =
  sweep 时该会话不再空闲且上轮在册。
- 计数 `buzhou.idle-session.entered`；无特征会话（未见过活动）不误报。
- 消费方：边界压缩（70）/归档（97）/排水（155）以本清单为候选输入。

## User Stories

1. 作为压缩策略，我按空闲清单提前折入长尾会话——活跃会话零打扰。
2. 作为运维，进入/离开空闲态的翻转流就是「会话库存周转」面板。
3. 作为宿主，一个阈值一个 sweep 调用——判据供给面统一，机制不再各记各的。

## Implementation Decisions

- 只判定不动作（动作归既有机制——分层诚实）；活跃事实只读特征仓快照。
- 空闲名册内存态（翻转判定用），LRU 1024 封顶。

## Testing Decimals

- 超阈进入空闲（清单+通知+排序）；活跃后离开（通知+下轮不列）；
  无特征会话不误报；阈值边界（=阈值即空闲）；多会话混合。

## Out of Scope

- 自动压缩/归档动作；定时装配；webhook 外发。

## Further Notes

- 水位消费链：特征事实（161）→ 水位判定（本轮）→ 压缩/归档动作（70/97）。
