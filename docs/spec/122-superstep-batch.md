# Spec 122 — 事务性并行批 · A 侧通用原语（effort #86）

> wayfinder map：`.wayfinder86/MAP.md`（T445–T446，A 会话半场）。#85 fog 种子
> 「事务性并行批（LangGraph superstep）」通用原语侧；harness 集成侧（原子前检 +
> BATCH_ABORTED）见 `122-atomic-superstep-batch.md`（B 会话，同轮分工）。
> 借鉴：LangGraph superstep 步内并行、失败即步失败。

## §A Problem Statement

一批可并行的任务（多工具并发取数、多路子代理召回）没有统一的全有或全无语义：
单路失败时调用方要自己拼「谁成了、谁没跑、谁在途」的去向拼图，部分结果容易
被误当成完整结果消费。

## §A Solution

`SuperstepBatch.runAll(superstepId, tasks, executor)`：完成序感知的快速失败——
任一任务失败<b>立即</b>中止在途（不等慢同伴）、已完成结果不可见（不返回部分
表）、异常 message 携带每任务去向（已完成被弃 / 被中止）。事务性口径诚实：
无回滚，副作用不撤销；「事务」= 失败快速传播 + 在途中断 + 结果可见性全有或
全无。失败折新错误码 `SUPERSTEP_FAILED`（NON_RETRYABLE：整批重放才有意义），
首败入错误签名族（kind=superstep），批级指标 `buzhou.superstep.outcome`
（outcome=ok|failed 有界二值）。空批零提交（不触碰执行器）。

## User Stories

1. 作为 agent 编排开发者，我用一个调用并发跑一批子任务，所以任一失败时我能从
   异常里直接读到失败者与被中止者，不用自己组装并发去向。
2. 作为 SRE，superstep 失败自动进错误签名族与批级指标，所以并行批的失败面与
   既有看板同一条管线。

## §B Testing Decisions

- 红队：完成序打乱返回仍按提交序；首败立即中断在途（慢任务被 interrupt）+
  去向清单入 message + 部分结果不可见；空批不触碰恶意执行器；参数 fail-fast。

## Out of Scope

- 回滚/补偿；重试策略（宿主组合 RetryPolicy）；按超时中止（TurnDeadline 组合）；
- harness 前检集成（B 侧持有）。

## Further Notes

- 与 B 侧正交：B 管「执行前派发门」，A 管「执行中批原语」；A/B 同 spec 编号
  双文件并存（MAP 分工登记），收口轮归一。
