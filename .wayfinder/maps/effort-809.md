# effort #809 — 作业死信台账

- 会话：H 会话 800 系第 10 轮 ｜ spec [809](../../../docs/spec/809-job-dead-letter.md) ｜ 票 [T1119](../tickets/T1119-job-dead-letter.md)/[T1120](../tickets/T1120-job-dead-letter-verify.md) ｜ impl562
- 借鉴：sidekiq dead set（mperham/sidekiq ≈13K star）——失败作业停尸待勘
- **换题注记**：原计划 R10 不可路由事件计数经勘察需动 DefaultAgentSession.deliverEvent 核心（读数侵入分发主路径）——按备选池纪律换入 S8 死信读数；原题留档待「监听者数量读数」类纯面方案。

## 勘察（排重）

- DelayedJobQueue（413）：one-shot 异常「吞+计数 buzhou.jobs.failed」——失败作业明细消失。
- WebhookDeadLetter（导出族）：webhook 域非延迟作业域。
- grep -i `deadletter|dead_letter`：webhook 族命中——作业域缺位。

## 决定

`JobDeadLetterLog`（core.concurrent）+ DelayedJobQueue 加可选观察者构造 `(Clock, BiConsumer<String,Throwable>)`（null=原行为零变化，2 参旧构造委托）：异常时回调 (jobKey, error)（观察者异常亦隔离）——台账环形留痕 64（挤最老）+按键聚合封顶 64（truncated）+lastErrorType+totalFailed；snapshot 只读（recent 新→旧、byKey 次数降序）；message 截 200；不重投（重投归提交方）。

## 测试

失败作业入账（单线程调度顺序 marker 保证）/聚合降序+lastErrorType+recent 新→旧/环挤老 64 精确+聚合 truncated/message 截 200/观察者抛异常隔离+失败计数照常+null 观察者零变化——5 例绿；DelayedJobQueueTest 既有 3 例回归绿（向后兼容）。

## 诚实边界

一次性语义无自动重投（sidekiq dead set 可 retry 是队列域差异——诚实声明）；进程内存有界（重启清零）；明细环与聚合键独立封顶（明细记而聚合 truncated——口径注记）。
