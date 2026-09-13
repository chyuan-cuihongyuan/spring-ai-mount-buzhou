# 950 — 摘要存储水位读面

> 来源：I 会话第 49 轮 = effort #950（impl 698）。水位系列第三站（spec 924 观测 / 925 索引 / 950 摘要）——容量治理读面收口。

## 背景

`InMemorySummaryStore` 新会话写入超 maxSessions 抛 QuotaExceededException（spec 13 §growth-8）——但「当前贴上限多少」无读面，容量前兆不可见。

## 目标

- `InMemorySummaryStore.watermark()`：`record Watermark(int activeSessions, int maxSessions)`——synchronized 读一致性；纯读面零行为变化。

## 兼容性

纯增量 internal 读面；零 API 面变化、零行为变化。
