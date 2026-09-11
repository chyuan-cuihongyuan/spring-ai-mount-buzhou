# Wayfinder Map — Buzhou 评估 error 项重试一次（effort #535，E 会话第 35 轮）

> E 会话第 35 轮（52 runner 扩散轮；抖动缓解）。勘察：judge 调用抖动/
> 网络闪断 → 项 error——重跑整跑成本高；error 项**重试一次**（语义 fail
> 不重试——重试会掩盖真实回归）空白。

## Destination

EvalRunner `setErrorRetryOnce(boolean)`（默认关）：runItemWithRetry 包装——
首跑 STATUS_ERROR → 重跑一次取第二次结果、detail 前缀 [RETRIED] 留痕 +
buzhou.eval.error-retried 计数；语义 fail 不重试（fail/error 三态语义
分明）；预算闸包装在外（重试也受预算约束）。

## Notes

- 号段：spec 535 / T823–824 / impl-437。
- 借鉴源：pytest flaky rerun / JUnit RetryRule。

## Out of scope

- 语义 fail 重试；多次重试；跨 run 自动重跑。

## Tickets

- [x] [T823 重试包装](../tickets/T823-error-retry-once.md)
- [x] [T824 语义 fail 不重试](../tickets/T824-semantic-fail-no-retry.md)
