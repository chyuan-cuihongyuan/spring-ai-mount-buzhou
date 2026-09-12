---
id: T894
title: 归档/还原互斥的形态裁决（勘察发现 restore 旁路事务）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

archive 走 CompensatingBatch saga（全局事务锁串行），restore 直写 store 不经事务——archive 与 restore 并发交错时：restore 可能在 archive 删活数据前读走归档键，随后 archive 删掉刚还原的数据而归档键已没了（数据丢失窗）。怎么收口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 23 轮 = effort #600 / spec 622 / impl 475）：

1. **每会话互斥锁**（ConcurrentHashMap<String,Object> 条目锁）：archive/restore 同会话串行、跨会话不受影响；锁粒度=会话数级、无 TT L 不泄漏问题（会话数有界）。
2. 勘察旁注入档：CompensatingBatch 走 UnitOfWork 全局锁——archive 之间本就全局串行（既有吞吐瓶颈，非本轮引入；per-session 事务化留雾区）；restore 旁路事务是丢失窗根因，互斥是最小修复。
3. purgeExpired 等只读/删归档键路径不加锁（不与活数据交错）。
