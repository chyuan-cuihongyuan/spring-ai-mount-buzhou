---
Type: task
Status: closed
---
## Question

`SessionStateStore.compareAndSwap(sessionId, key, expectedValue, update)`：expected 为 null
= 键不存在才写。默认实现 check-then-put（诚实标注仅单实例语义）；InMemory 以
computeIfPresent/compute 原子；JDBC 条件 UPDATE（value 相等）+ expected=null 时条件
INSERT（各单语句影响行数判定）；Redis 单 Lua（DEL+HSET+SADD）。返回是否交换成功。

## Resolution

done（2026-08-29）：见 MAP Decisions 与 spec 56 对应节；实现/测试/文档随本 effort 提交入档。
