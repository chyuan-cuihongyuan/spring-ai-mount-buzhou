# Wayfinder Map — Buzhou session.opened 生命周期事件补齐（effort #522，E 会话第 23 轮）

> E 会话第 23 轮。勘察：事件族有 session.closed/forked/lease.lost/
> hook.blocked/turn.*——**session.opened** 缺失：spawn 时刻（谁在何时
> 开了什么会话）无事件，生命周期首尾不成对（webhook 订阅方收不到开场）。

## Destination

DefaultAgentRuntime.doSpawn 尾部（监听器挂载后、返回前）经
session.dispatchEventInternal 派发 `session.opened`（payload 身份三元组
appId/agentName/sessionId——全局监听无隐式会话上下文故显式携带）。
与既有 session.closed 配对闭环。

## Notes

- 号段：spec 522 / T795–T796 / impl-425。
- 无新类型（内部加法变更——快照零 diff）。

## Out of scope

- closed 事件补 payload（兼容面不动）；steal 接管专属事件。

## Tickets

- [x] [T795 spawn 派发缝](../tickets/T795-session-opened-event.md)
- [x] [T796 payload 身份三元组](../tickets/T796-session-opened-payload.md)
