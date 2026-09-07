---
Type: task
Status: closed
---
## Question

合并位 id 重写正确性 + 取消桥接（共享 Future cancel → 底层任务中断）回归。

## Resolution

done（2026-09-01）：impl-323；`ToolCallCoalescer` 补取消桥接（whenComplete
CancellationException → underlying.cancel(true)）；测试断言各回喂位 id 与
调用位一致、超时取消后底层阻塞任务被中断。
