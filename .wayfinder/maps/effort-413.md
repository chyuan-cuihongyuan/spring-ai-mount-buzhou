# Wayfinder Map — Buzhou 延迟作业原语（effort #413，D 会话第 14 轮）

> D 会话第 14 轮。勘察（2026-09-08）：grep 调度面——SleepTimeScheduler
> 是 memory 内部 consolidation 专用；后台家务族（331 选主族）各自为政；
> **通用延迟作业原语缺失**：「N 分钟后跑一次」/「到期执行一次」的
> 定时/延迟语义无处安放（Sidekiq delayed_jobs / Quartz simple trigger
> 无对应物）。

## Destination

`core.concurrent.DelayedJobQueue`（Sidekiq delayed_jobs 借鉴——到点执行
一次）：`submit(jobKey, task, fireAt|delay)`（同 key 重复提交 = 替换旧
任务——键即幂等锚）；`cancel(jobKey)`；单 daemon 虚拟线程调度器
（ScheduledExecutorService，close 即停——作业不等待完成）；作业异常
隔离（吞 + 计数 `buzhou.jobs.failed`，不炸调度线程）；`pending()` 待跑
清单快照（jobKey + fireAt——观测面）；Clock 注入测试确定性。原语先行
（具体作业面——延迟工具调用/重试家务——扩散候选）。

## Notes

- 号段：spec 413 / T717–T718 / impl-386。
- 借鉴源：Sidekiq（13k★）delayed_jobs（perform_in/perform_at）；Quartz
  simple trigger 的 one-shot 语义。
- 纪律：one-shot（重复语义=显式再提交）；进程内（重启丢作业——诚实
  边界；持久化扩散候选）；任务引用不留快照（pending 只留键与时间）。

## Out of scope

- 持久化/跨实例作业；cron 周期语义；作业优先级（PriorityLane 组合用）；
- 作业重试策略（宿主自理）。

## Tickets

- [x] [T717 DelayedJobQueue 原语](../tickets/T717-delayed-job-queue.md)
- [x] [T718 替换/取消/隔离语义](../tickets/T718-delayed-job-semantics.md)
