---
Type: task
Status: closed
blocked-by: T249
---
## Question

`SessionStateHandle.compareAndSwap(key, expected, update)`（HookEnvironment 以 put 同口径
构造 StateEntry 透传 store CAS）；`SessionQuotaHook` 三处 read-then-put 改有界 CAS 重试
（16 次；日翻越 expect stale 原串；耗尽回退 last-write 覆写 + stats 记回退次数）。
拦截点/事件/文案零变化；sessionLocks 保留为 JVM 内默认实现兜底。

## Resolution

done（2026-08-29）：见 MAP Decisions 与 spec 56 对应节；实现/测试/文档随本 effort 提交入档。
