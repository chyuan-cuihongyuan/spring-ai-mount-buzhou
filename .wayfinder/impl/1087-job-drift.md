# 1087 — 延迟作业调度漂移读数

**What to build:** DelayedJobQueue 增量（task 包装漂移记录+DriftStats+resetDriftForTest）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] submit 双重载 task 包装（clock 注入一致性，负值钳 0）
- [x] DelayedJobQueueDriftTest 五测
- [x] spec 1434 + README 行（既有类增量——快照面不变）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='DelayedJobQueueDriftTest,DelayedJobQueueTest'` 8/8 绿。
