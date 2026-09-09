# Spec 413 — 延迟作业原语（effort #413）

> wayfinder map：`.wayfinder/maps/effort-413.md`（T717–T718）。D 会话第 14 轮。

## Problem Statement

通用延迟作业原语缺失：「N 分钟后跑一次」/「到期执行一次」的定时/延迟
语义无处安放——SleepTimeScheduler 是 memory 专用，各家务自管线程；
宿主想延迟执行清理/重试/提醒只能自造轮子。

## Solution

`core.concurrent.DelayedJobQueue`（Sidekiq delayed_jobs 借鉴——one-shot）：

- **`submit(jobKey, task, fireAt)`** / `submit(jobKey, task, delay)`：
  到点执行一次；**同 key 重复提交 = 替换旧任务**（键即幂等锚——重提
  交刷新时间不双跑）。
- **`cancel(jobKey)`**：撤销（幂等；已跑/不在 = no-op）。
- **执行**：单 daemon 虚拟线程调度器（ScheduledExecutorService）；
  作业异常隔离（吞 + 计数 `buzhou.jobs.failed`——不炸调度线程）。
- **`pending()`**：待跑清单快照（jobKey + fireAt 升序——观测面；任务
  引擎/闭包不外泄）。
- **`close()`**：停调度器（在途作业不等待——与生命周期族口径一致）。
- Clock 可注入（测试确定性）；进程内（重启丢作业——诚实边界）。

## User Stories

1. 作为宿主开发者，我想一行提交延迟任务，so 不自造调度线程。
2. 作为宿主开发者，我想同键重提交刷新而不双跑，so 重试/提醒类任务
   天然幂等。
3. 作为运维，我想待跑清单可查、失败有计数，so 作业积压/失败可见。

## Implementation Decisions

- one-shot（重复 = 显式再提交）；无 cron。
- pending 不暴露任务引用（观测面最小化）。

## Testing Decisions

- 到点执行（Clock/短延迟 + await）；同键替换不双跑；cancel 后不跑；
- 异常隔离（后续作业照常 + failed 计数）；pending 升序快照；close 幂等。

## Out of Scope

- 持久化；跨实例；cron；优先级；重试策略。

## Further Notes

- 新公共类型 `DelayedJobQueue`（嵌套 `PendingJob`）随轮 regenerate 快照。
