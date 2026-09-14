---
id: T2172
title: 调度漂移计数与迟到显形的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2171
created: 2026-09-14
---

## Question

如何证明漂移采样、过期补跑显形与替换不双跑共存？

## Resolution

**用户常设授权 AFK（可推翻）**

`DelayedJobQueueDriftTest` 五测全绿（`mvn -pl buzhou-core -am test`）：双作业执行计数+漂移≥0；**过期 60s 补跑漂移≥59s 显形**（补偿错过窗口量化）；替换旧任务不双跑且 executed=1；reset 归零；Duration 重载同被计量。DelayedJobQueueTest 回归绿。
