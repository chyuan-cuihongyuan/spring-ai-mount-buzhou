---
id: T2191
title: 媒体摄入统计读面（MediaIntake 增量）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 45 轮（换题轮）：多模态摄入画像面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：IdempotencyAdvisor 复用 ResponseCacheStore 计数已覆盖幂等读面——换入 MediaIntake 摄入统计轴（多模态画像+字节配额治理）。

形状裁决：MediaIntake 实例面增量——intakes/bytesTotal/readBacks 三计数+per-MIME 直方（数量降序典序、封顶 16 基数纪律）+stats()/resetStatsForTest（不影响 store）；intakeText 复用 intake 同一漏斗自动计量；拒绝路径不入账。

Out of scope：会话分桶；字节分位；内容审查。
