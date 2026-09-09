---
Type: task
Status: closed
---
## Question

`ErrorBudgetPolicy`（周期评 anyBreaching：烧穿→HIGH、连续两轮清明→解冻
防抖、WARN+计数、SmartLifecycle 自管虚拟线程）+ yml 装配
（buzhou.backpressure.error-budget-freeze.{enabled,interval}）+ 收口。

## Resolution

done（2026-09-04）：impl-358；政策五用例（烧穿冻结/单轮清明不解冻/
两轮清明解冻/计数留痕/stop 停轮询）+ 装配三用例（启用装配/缺省零
变化/无 ErrorBudget 启动红）绿；runtime 装配线接地板注入；快照
regenerate +3 型；README 纵深 IV 加行、覆盖门绿；顺带 .wayfinder334
号段归位（R35 目录误名修正随本轮提交）。
