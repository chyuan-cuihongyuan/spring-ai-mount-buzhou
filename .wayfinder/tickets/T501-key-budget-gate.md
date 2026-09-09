---
Type: task
Status: closed
---
## Question

key 级预算闸：VirtualKeys 接 TokenBudgetHook + 耗尽态语义。

## Resolution

done（2026-08-30）：impl-286；TokenBudgetHook 5 参构造 + EVENT_KEY_HARD_STOP
双面接线 + VirtualKeys.isExhausted（越限锁定至 reset）+ e2e 2 例 + 回归。
