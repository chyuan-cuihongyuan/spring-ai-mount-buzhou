# Spec 108 — 工具调用时长 timer（effort #70）

> wayfinder map：`.wayfinder/maps/effort-70.md`（T397–T398）。借鉴：Micrometer timer /
> Prometheus histogram（P95 慢工具告警）。

## Problem Statement

工具面只有调用计数（buzhou.tool.calls outcome=ok|failed）——「哪个工具慢」不可
聚合：慢工具（外部 HTTP 超时前兆、大结果拖慢 Turn）检测无 timer 维。

## Solution

HookedToolCallback 在 delegate.call 外围 System.nanoTime 计时：timer
`buzhou.tool.duration`（tag outcome=ok|failed——慢工具与失败工具延迟可分；工具名
不进 tag——无界纪律）。既有 counter、错误即反馈通道、签名聚类零变化。

## User Stories

1. 作为运维，我要 P95 工具延迟可告警，所以外部依赖劣化提前可见。
2. 作为红队，我要失败延迟单独可分，所以「慢了才失败」与「秒败」可辨。

## Implementation Decisions

- 计时覆盖 delegate 本体（不含 hook 链——hook 延迟归属 fog 另议）。
- 全工具统一（所有机制工具经本回调——既有 counter 同点同源）。

## Testing Decisions

- ok/failed 双计时各一（tag 正确 + 纳秒非负）；既有 counter 面回归不变。

## Out of Scope

- per-tool 进程内表；histogram bucket 配置。

## Further Notes

- 与 spec 83/104 错误族、99 折入族组成「计数+时长+签名」三维修测面。
