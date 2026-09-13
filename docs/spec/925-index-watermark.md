# 925 — 会话索引存量水位读面

> 来源：I 会话第 26 轮 = effort #925（[T1301](../../.wayfinder/tickets/T1301-index-watermark-shape.md) / [T1302](../../.wayfinder/tickets/T1302-index-watermark-verify.md) / impl 678）。spec 924 水位读面同构扩散（Redis INFO 直觉延续）。

## 背景

`InMemorySessionIndexStore` 是会话治理第一查询面（分页/游标/spec 631）。「索引里有多少会话条目、贴近容量了吗」无读面——索引贴顶意味着新会话不可发现（治理前兆）。

## 目标

- `InMemorySessionIndexStore`（internal）新增 `watermark()`：
  - `record Watermark(int indexedSessions, int maxSessions)`——indexedSessions 为索引条目数；maxSessions 直通实现上限概念（无独立上限 = -1 显式无界标注，诚实口径入档）；
  - synchronized/并发安全读；纯读面零行为变化。

## 兼容性

纯增量 internal 读面；零 API 面变化、零行为变化。
