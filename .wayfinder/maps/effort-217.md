# Wayfinder Map — Buzhou 轮内模型调用循环闸（effort #217，B 会话第 40 轮）

> B 会话第 40 轮。runaway 管轮内<b>工具步数</b>循环；模型调用自身的循环
> （advisor 重试+REASK 环路把模型调了 30 次）没有独立闸——重试预算管校验
> 反馈一种来源，其他环路（onModelError 兜底再调等）无总闸。

## Destination

ModelCallCapHook（core/hook）：beforeModel 按 (session, turn) 计数，超上限
（默认 32）block 可读理由（模型调用循环疑——附计数）；afterTurn 清零；
计数 buzhou.model-cap.blocked。最后一道兜底闸，不管环路成因。

## Notes

- 号段：B=奇数 spec（本轮 197）；轮次 .wayfinder200+。
- order 20（最早——循环闸先于一切策略面）。
- 内存表 LRU 1024（会话×轮 键天然随轮滚动）。

## Decisions so far

- 只数模型调用本身（工具循环归 runaway）。

## Not yet specified

- 上限配置面；循环事件外发（含最近调用摘要）。

## Out of scope

- 沿用各轮；跨轮累计（检疫 143 管那个面）。

## Tickets

- [x] [T569 ModelCallCapHook（轮内模型调用总闸）](../tickets/T569-model-cap.md)（impl-312）
- [x] [T570 循环闸回归（超限拦/清零/隔轮独立/null 安全）](../tickets/T570-model-cap-tests.md)（impl-312）
