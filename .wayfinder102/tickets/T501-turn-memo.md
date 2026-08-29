---
Type: task
Status: closed
---
## Question

轮作用域 memo 表 + 装饰器 + 轮清零 hook。

## Resolution

done（2026-08-30）：impl-286；TurnMemo（computeIfAbsent + 失败不 memo + 计数）
+ MemoizedToolCallback（name+argsHash 键，定义透传）+ TurnMemoHook（order 30 清零）。
