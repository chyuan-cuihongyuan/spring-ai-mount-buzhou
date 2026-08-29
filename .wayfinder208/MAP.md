# Wayfinder Map — Buzhou 空闲会话水位监控（effort #208，B 会话第 31 轮）

> B 会话第 31 轮。主题池「空闲水位清扫」：空闲会话（数小时无活动）占着内存
> 特征/上下文，但「谁空闲多久」没有统一水位视图。借鉴 Flink watermark
> （事件时间水位推进判定迟到/空闲）。

## Destination

IdleSessionMonitor（core/session，组合 SessionFeatureStore 特征面）：
sweep(now) → 空闲超阈值的会话清单（id + idleMillis）+ 翻转通知
（进入/离开空闲态）+ 计数——压缩/归档候选的统一供给面。

## Notes

- 号段：B=奇数 spec（本轮 179）；轮次 .wayfinder200+。
- 组合而非新采集：活跃事实来自特征仓 lastActiveAt（spec 161）。
- 与边界压缩（70）/归档（97）正交：那是动作，这是判据供给。

## Decisions so far

- 翻转才通知（与工具探测同纪律——不刷屏）。

## Not yet specified

- sweep 定时装配；空闲事件 webhook 外发。

## Out of scope

- 沿用各轮；空闲即自动压缩（动作归既有机制接线）。

## Tickets

- [x] [T551 IdleSessionMonitor（水位判定+翻转通知）](tickets/T551-idle-monitor.md)（impl-303）
- [x] [T552 水位回归（超阈进入/活跃离开/无特征零误报）](tickets/T552-idle-tests.md)（impl-303）
