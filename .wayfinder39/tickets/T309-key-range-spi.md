---
Type: task
Status: closed
---
## Question

scanByKeyRange SPI + 三栈语义。

## Resolution

done（2026-08-29）：impl-225；默认排序兜底 + JDBC ORDER BY/BETWEEN/LIMIT 下推 +
内存键迭代覆写；Redis 走默认（fog 记账）。
