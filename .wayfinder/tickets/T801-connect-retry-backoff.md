---
Type: task
Status: closed
---
## Question

addEntryWithRetry：base×2^(n-1) 封顶 60s scheduler 重排+耗尽收口既有
失败语义+retries 计数；策略 record 校验 fail-fast。

## Resolution

done（2026-09-12）：impl-427；退避成功/耗尽/校验用例绿。
