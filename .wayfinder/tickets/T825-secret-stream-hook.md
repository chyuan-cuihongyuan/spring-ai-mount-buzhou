---
Type: task
Status: closed
---
## Question

窗口流式秘密扫描（SecretScanner 复用+占位符不拆分+flush 排空）+命中
计数复用 secret.redactions。

## Resolution

done（2026-09-12）：impl-438；跨 chunk/flush/恒等用例绿。
