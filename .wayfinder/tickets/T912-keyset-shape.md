---
id: T912
title: 会话索引 keyset 游标分页的裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

SessionIndexQuery 是 offset 分页——活跃度变化的索引上翻页会跳行/重行（第 2 页 offset=20 时前 20 行有人刷新就整体位移）。keyset 怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 32 轮 = effort #600 / spec 631 / impl 484）：

1. `SessionIndexQuery.cursor`（第 8 组件，七参兼容保留）：不透明 Base64("lastActiveAtMs:sessionId")，自校验 fail-fast；`encodeCursor(上页末行)` 便捷工厂。
2. **规范序统一三实现**：(lastActiveAt DESC, sessionId ASC)——CANONICAL_ORDER 常量 + afterCursor 谓词入 spi；Redis 实现顺带修正（zset 平局序与规范序不同，收集后重排——原实现翻页本就与内存不一致的隐性偏差一并收口）。
3. JDBC SQL 下推（`< lat OR (= lat AND > id)`）；内存/Redis 收集后过滤；cursor 与 offset 可叠加（cursor 定锚 offset 再跳）。
