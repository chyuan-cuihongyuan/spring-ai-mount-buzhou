---
Type: task
Status: closed
---
## Question

AgentBulkhead.resize（ResizableSemaphore 扩/缩/加/摘 + 拒绝计数保留 +
buzhou.bulkhead.resized 计数）+ BuzhouConfigRefreshEvent（空标记事件）+
BulkheadHotReload 监听器（Binder 重读 buzhou.bulkhead.agents → resize
全局舱；装配随舱开）。

## Resolution

done（2026-09-02）：impl-343；resize 五用例绿（缩容语义：在飞须**低于**
新限才放新——=限仍拒，诚实归档）。
