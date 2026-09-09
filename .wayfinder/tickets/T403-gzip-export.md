---
Type: task
Status: closed
---
## Question

gzip 双重载。

## Resolution

done（2026-08-30）：impl-256；GZIP+UTF-8 Writer 复用既有管线，逐字节一致。
