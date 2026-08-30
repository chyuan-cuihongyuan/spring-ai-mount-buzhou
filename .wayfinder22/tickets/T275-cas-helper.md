---
Type: task
Status: closed
---
## Question

core.internal.hook.AtomicStateCounters.swapValue(handle, key, nextOf, onFallback)——
进度检测 CAS + 停滞 16 回退；TokenBudgetHook.stateAdd / RunawayHook.incrementSessionCounter /
SessionQuotaHook 增量与累计统一改走助手。

## Resolution

done（2026-08-29）：impl-208；三 Hook 统一 + 并发红队 2 例 + quota 回归绿。
