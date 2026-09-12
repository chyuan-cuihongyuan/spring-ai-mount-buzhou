# Spec 535 — 评估 error 项重试一次（effort #535）

> wayfinder map：`.wayfinder/maps/effort-535.md`（T823–T824）。E 会话第 35 轮。

## Problem Statement

judge 抖动/网络闪断 → 项 error——重跑整跑成本高。error 项重试一次
（语义 fail 不重试）的抖动缓解空白（pytest flaky rerun 语义）。

## Solution

EvalRunner `setErrorRetryOnce(boolean)`（默认关）：runItemWithRetry
包装——首跑 STATUS_ERROR → 重跑一次取第二次结果、detail 前缀
`[RETRIED]` 留痕 + `buzhou.eval.error-retried` 计数；语义 fail 不重试
（fail/error 三态分明——重试语义失败会掩盖真实回归）；预算闸包装在外
（重试受预算约束）。

## User Stories

1. 作为评测方，我想 judge 抖动的 error 项自动重跑一次， so 偶发抖动
   不污染 run 结论（无需整跑重跑）。

## Implementation Decisions

- 仅 STATUS_ERROR 重试（语义 fail 不重试——掩盖真实回归）。
- detail 前缀留痕（重跑发生过可追溯）。

## Testing Decisions

- FlakyOnceEvaluator：首跑 error → 重试 pass + [RETRIED] + calls 2；
- AlwaysFailEvaluator：fail 不重试 calls 1；默认关零变化。

## Out of Scope

- 语义 fail 重试；多次重试；跨 run 自动重跑。

## Further Notes

- 无新顶层公共类型——快照零 diff 预期。
