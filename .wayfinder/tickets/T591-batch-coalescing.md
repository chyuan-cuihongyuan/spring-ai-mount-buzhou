---
Type: task
Status: closed
---
## Question

批内同工具同参调用执行一次、全部位共享值回喂（139 原语接线执行脊柱）。

## Resolution

done（2026-09-01）：impl-323；`HarnessToolCallingManager.setBatchCoalescer(...)`
（默认关）+ 批派发走 `coalescer.submit`，键=工具名+全参串；合并位回喂逐位重写
id。`HarnessBatchCoalescingTest` 四象限回归绿。
