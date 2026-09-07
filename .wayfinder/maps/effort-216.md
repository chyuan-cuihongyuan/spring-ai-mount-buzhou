# Wayfinder Map — Buzhou 降级链演练（effort #216，B 会话第 39 轮）

> B 会话第 39 轮。离群驱逐（149）被动等错误；「备模型真的可用吗」平时不知道
> ——真降级时才发现备模型凭证过期/配额没了。借鉴 Envoy 主动健康检查
> （平时探活，用时可靠）。

## Destination

FallbackDrill（resilience/fallback）：宿主给轻量演练探针（每模型一句便宜调用）
——drillAll 记 lastVerifiedAt；isFresh(model, maxAge) 判新；filter 剔除过期未验
模型。与驱逐（149）互补：驱逐管「坏了的」，演练管「验证好的」。

## Notes

- 号段：B=奇数 spec（本轮 195）；轮次 .wayfinder200+。
- 探针归宿主（每模型的便宜验证方式自己清楚）；异常记 lastFailedAt 不上抛。
- 演练有真实成本——间隔与 maxAge 归宿主权衡（诚实边界）。

## Decisions so far

- 演练失败不驱逐（那是 149 的职责）——只标记未验证。

## Not yet specified

- 定时演练调度；演练结果事件外发。

## Out of scope

- 沿用各轮；自动切换；演练流量采样。

## Tickets

- [x] [T567 FallbackDrill（探针演练+新鲜度+过滤）](../tickets/T567-drill.md)（impl-311）
- [x] [T568 演练回归（成功记新/失败记败/过期剔除/异常吞）](../tickets/T568-drill-tests.md)（impl-311）
