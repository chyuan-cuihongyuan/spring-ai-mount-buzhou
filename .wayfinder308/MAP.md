# Wayfinder Map — Buzhou deadline 跨工具传播（effort #308，C 会话第 9 轮）

> C 会话第 9 轮。Turn 硬 Deadline（spec 13 §core-2）由管理器强制，但工具
> <b>看不见</b>剩余预算——自限型工具（搜索翻页/批量拉取）无法按剩余时间
> 自我收敛，只能被硬取消。

## Destination

`ToolContext` 携带当前 TurnDeadline（动态视图——remainingMillis 实时递减）；
管理器提供 `turnDeadlineOf(toolContext)` 静态取用（sessionIdOf 同款）；
无 Deadline 时哨兵 none() 透传（工具自查 isNone 自由放行）。

## Notes

- 借鉴：gRPC deadline 逐跳传播（deadline 属于调用链，每一跳都能看到剩余）。
- 号段：spec 308 / T607–T608 / impl-331。

## Decisions so far

- 直接透传 TurnDeadline 对象（core.session 公共类型，动态视图免快照失真）。
- 键 `buzhou.turnDeadline`；哨兵 none() = 无限等待既有语义。

## Out of scope

- 跨进程传播（MCP 出站带剩余预算头——归 MCP 族后续轮）。

## Tickets

- [x] [T607 ToolContext 携带 TurnDeadline + 取用助手](tickets/T607-deadline-ctx.md)（impl-331）
- [x] [T608 自限工具回归（读剩余收敛/无 Deadline 放行）](tickets/T608-deadline-close.md)（impl-331）
