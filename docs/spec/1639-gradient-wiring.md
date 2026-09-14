# 1639 · 梯度限流器观测接线（spec 1617 装配面）

> 来源：N 会话 R40（effort #1639 / T2429–T2430 / impl 1192）。

## Solution

`GradientLimiterHolder`（进程级，默认 Config.defaults）+
`executeToolCalls` 包装：批耗时（nanoTime 差）喂 `limiter().record(ms)`
——finally 路径保证取消/异常也入账。**观测先行**：View 读数（两 EMA/
gradient/limit）显形全局工具路径的延迟梯度；tryAcquire 闸接入（批并发
准入）待数据积累后独立裁决（行为面大不与观测混轮）。

## Testing Decisions

- `GradientLimiterWiringTest` 两断言：Holder 喂入后 View 双 EMA=100、
  gradient=1.0；install 替换与 null 重置（默认 min=4 起步）。
- 回归：GradientAdaptiveLimiterTest 7 用例。

## Out of Scope

- tryAcquire 闸接入（批准入行为变更——独立裁决）。
- per-agent 梯度限流（进程级先行——工具路径是进程共享资源）。
