# Spec 522 — session.opened 生命周期事件补齐（effort #522）

> wayfinder map：`.wayfinder/maps/effort-522.md`（T795–T796）。E 会话第 23 轮。

## Problem Statement

事件族有 session.closed/forked/lease.lost——**session.opened** 缺失：
spawn 时刻无事件，生命周期首尾不成对（webhook 订阅方收不到开场通知，
「谁在何时开了什么会话」不可订阅）。

## Solution

DefaultAgentRuntime.doSpawn 尾部（listeners 挂载后、返回前）派发
`session.opened`（payload：appId/agentName/sessionId 身份三元组——
全局监听无隐式会话上下文故显式携带）。经 session 级 dispatchEventInternal
——SYNC/buffered 两种分发模式自动继承。

## User Stories

1. 作为运维，我想订阅 session.opened， so 会话开场可观测可告警
   （配合 closed 成对审计在线时长）。

## Implementation Decisions

- 监听器挂载后派发（全局监听/webhook 均可达——派发时序正确性）。
- payload 显式身份三元组（SessionEvent 无内嵌 session id——全局监听
  需要）。

## Testing Decisions

- spawn 即派发（先于任何轮次）；closed 在 opened 之后（配对）；payload
  三元组断言。

## Out of Scope

- closed 事件补 payload；steal 专属事件。

## Further Notes

- 无新顶层公共类型——快照零 diff 预期。
