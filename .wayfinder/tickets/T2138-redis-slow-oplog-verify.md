---
id: T2138
title: 慢操作榜阈值/FIFO/水位语义的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2137
created: 2026-09-14
---

## Question

如何证明严格大于、FIFO 挤旧、水位与新→旧序？

## Resolution

**用户常设授权 AFK（可推翻）**

`RedisSlowOpLogTest` 五测全绿（`mvn -pl buzhou-store-redis -am test`）：阈值下忽略；恰等于不入（严格大于）；37 条压环 FIFO 挤旧 5 条（评审修正：初版断言 op4 差一实为 op5，且实证实现序与契约相反——descendingIterator 修正）+水位=累计；阈值动态调整双向；reset 全复位含阈值。RedisMessageStore 埋点为 finally 计时（慢与败正交）。
