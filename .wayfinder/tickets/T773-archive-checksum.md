---
Type: task
Status: closed
---
## Question

archive() 写时 sha256 校验和落独立命名空间 + verify 五态（OK/MISMATCH/
NO_CHECKSUM/CORRUPT/NO_ARCHIVE）+ verifyAll。

## Resolution

done（2026-09-12）：impl-414；五态用例绿（篡改/存量/损坏模拟）。
