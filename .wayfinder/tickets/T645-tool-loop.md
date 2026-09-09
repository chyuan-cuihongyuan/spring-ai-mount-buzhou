---
Type: task
Status: closed
---
## Question

ToolLoopBreakerHook（order 245：toolName+args hashCode 相邻 run 达窗
block 持续干预直到换 key、干预指令文案、per-session 1024 重置、计数器）
+ 属性 + 装配（window 未配不装）。

## Resolution

done（2026-09-02）：impl-350；hook 六用例绿（Map.hashCode 顺序无关性
专测）。
