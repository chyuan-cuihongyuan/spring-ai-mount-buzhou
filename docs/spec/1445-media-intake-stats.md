# 1445 — 媒体摄入统计读面

> 来源：L 会话第 45 轮 = effort #1445（票 T2187 兄弟 / T2188 / impl 1097）。**换题记录**：R45 原题勘察发现 IdempotencyAdvisor 复用 ResponseCacheStore 计数已覆盖——换入多模态摄入轴。借鉴：OpenAI usage by modality（按模态拆分用量——图片/音频/文本的消费画像）。

## Problem Statement

`MediaIntake`（多模态摄入：图片/音频转 spill 引用）的摄入量无读面：多少媒体、多少字节、什么 MIME 类型占主导——多模态使用画像与字节配额治理（大图片吃 spill 配额）无数据。

## 目标

- `MediaIntake`（spill，实例面——包装具体 store）增量：
  - `intakes`（摄入条目数，intakeText 复用同一漏斗）/ `bytesTotal`（字节精确累计）/ `readBacks`（回读次数）；
  - per-MIME 计数直方（数量降序平名典序；**封顶 16** 超出丢弃不记——基数纪律）；
  - `stats()` → `record MediaIntakeStats(intakes, bytesTotal, readBacks, byMime)` + `resetStatsForTest()`（不影响 store）。
- intake 拒绝路径（空字节 IllegalArgumentException）不入账。

## 兼容性

纯增量读面：intake/readBack 存储语义逐位不变；未读快照零开销。

## Out of Scope

- 按会话/agent 分桶（基数红线）。
- 字节分位（摄入直方即可）。
- 媒体内容审查（moderation 域）。
