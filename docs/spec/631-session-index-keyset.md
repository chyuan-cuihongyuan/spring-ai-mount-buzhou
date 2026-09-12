# 631 — 会话索引 keyset 游标分页

> 借鉴：[postgresql](https://www.postgresql.org/docs/current/limits.html) keyset pagination 惯例（行序键锚定翻页）。
> 来源：F 会话第 32 轮 = effort #600 / [T912](../../.wayfinder/tickets/T912-keyset-shape.md) / [T913](../../.wayfinder/tickets/T913-keyset-verify.md) / impl 484。

## 背景

offset 分页在活跃度变化的索引上跳行/重行（第二页 offset=20 时前 20 行刷新即整体位移）——面板/巡检翻页口径不稳。

## 目标

`SessionIndexQuery.cursor`（不透明自校验）+ 三实现一致支持。

## 非目标

- 不移除 offset（低频静态索引够用；cursor 为稳定翻页的可选项）。

## 设计

规范序 (lastActiveAt DESC, sessionId ASC) 统一三实现（Redis zset 平局序偏差一并收口）；JDBC SQL 下推；内存/Redis 收集后过滤；cursor+offset 可叠加。

## 测试

内存 3 用例（稳定/平局/codec）+ JDBC SQL 下推 1 用例 + 双 store 契约零回归。

## 兼容性

七参构造保留；默认 null 游标 = 既有 offset 语义零变化。
