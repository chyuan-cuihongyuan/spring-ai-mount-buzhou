# 606 — 取消原因枚举传播

> 借鉴：[grpc/grpc](https://github.com/grpc/grpc) status codes——取消以稳定机器可读闭集刻画「谁/为什么」。
> 来源：F 会话第 7 轮 = effort #600 / [T862](../../.wayfinder/tickets/T862-cancel-cause-shape.md) / [T863](../../.wayfinder/tickets/T863-cancel-cause-verify.md) / impl 459。

## 背景

`session.cancelled` 事件只有 cancelMode（何时停：立即/工具批后/本轮后），没有取消发起者的维度——「用户主动按停」（正常）与「停机排水收割」（运维事件）在观测面不可区分，告警与排障口径混流。

## 目标

`CancelCause` 闭集枚举 + `cancel(CancelMode, CancelCause)` API：cause 进事件 payload 与指标 `buzhou.session.cancelled`（tag: cause）。

## 非目标

- 不改取消三档语义（CancelMode 口径零变化）。
- DEADLINE / LEASE_LOST / RUNAWAY 当前各走异常路径（TIMEOUT / LeaseLostException / runaway Block），不改道 cancel——枚举先占位供后续接入。

## 设计

- 枚举：USER（默认——无法区分时的诚实缺省）/ SHUTDOWN_DRAIN / DEADLINE / LEASE_LOST / RUNAWAY。
- 接口 default 方法委托旧路径（既有实现零改动）；DefaultAgentSession 覆写带因版本。
- 停机排水（runtime shutdown 对在途 Turn 的强制收敛）显式传 SHUTDOWN_DRAIN。

## 测试

CancelCauseTest 3 用例（默认 USER / 显式 DEADLINE / null 缺省）+ core 全模块零回归。

## 兼容性

事件 payload 加字段（消费方按 key 取值不受影响）；新指标纯增量；接口 default 方法二进制兼容。
