# Spec 155 — 会话优雅排水（effort #106）

> wayfinder map：`.wayfinder/maps/effort-106.md`（T513–T514）。借鉴：K8s pod drain
> （Terminating 拒新 + 等端点排空再逐出）——维护下线从「硬斩」变「排空」。

## Problem Statement

会话要下线做维护（归档/迁移/租户退订）：现在只能 close——在飞 Turn 直接被斩，
用户看到的是断崖。K8s 对 Pod 的解法是排水：先标 Terminating（新请求被拒）、
等在飞请求排空、再真正逐出。

## Solution

`SessionDrainCoordinator`（core/session）：

- **beginDrain(sessionId)**：进入排水态——此后 `enter(sessionId)` 抛
  `BuzhouException(SESSION_DRAINING)`（新 Turn 拒绝，可读理由）。
- **在飞记账**：`enter` 返回 AutoCloseable `Lease`（Turn 开始取、结束还）；
  排水前已取的 Lease 正常走完。
- **等排空**：`awaitDrained(sessionId, timeout)`——在飞归零即 true；
  超时 false（<b>不死等</b>——调用方决定升级硬关，诚实边界）。
- 观测：`drainingSessions()` 名单 + 各自在飞数。

## User Stories

1. 作为运维，维护窗口先 drain 再归档再关——在飞轮走完，用户无断崖感知。
2. 作为宿主，排水期新请求收到明确「维护中」拒绝（不是超时挂死）。
3. 作为运维，awaitDrained 超时即知有长轮卡住——升级决策有依据。

## Implementation Decisions

- per-session 状态（draining + in-flight 计数 + CountDownLatch 排空信号）；
  Lease.close 归零时 countDown。
- ErrorCode 追加 SESSION_DRAINING（NON_RETRYABLE）。

## Testing Decisions

- 未排水 enter 正常；排水后 enter 拒（错误码与文案）；在飞 Lease 完成后
  awaitDrained true；有在飞时 await 超时 false；多 Lease 全还才排空；
  drainingSessions 名单。

## Out of Scope

- 归档流水线串联；跨实例迁移；自动硬关升级。

## Further Notes

- 会话生命周期：spawn 闸（背压）→ 检疫（143）→ 排水（本轮）→ 归档（97）。
