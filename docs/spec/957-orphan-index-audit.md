# 957 — outbox due 索引孤儿审计

> 来源：I 会话第 56 轮 = effort #957（impl 694 续）。配对完整性思想（spec 735 事件配对审计在 outbox 索引域的应用）。

## 背景

outbox 主记录（outbox.*）与 due 索引（due.*）成对维护（append/update/markDead 双写双删）——删除时序缺陷会留下「索引在、主记录无」的孤儿：投递扫描空转 + 存储慢性泄漏。

## 目标

- `WebhookOutbox.orphanIndexCount()` 包级读面：扫 due 前缀，indexEntry 指向的主记录缺失的条目数（synchronized 计数语义）；
- 正常路径（append/appendRetry/markDead 一致记录）零孤儿断言固化；
- 人为注入孤儿可检出（审计探针口径）。

## 兼容性

纯读面；零行为变化。
