---
id: T1177
title: 限流键热点读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

限流键热力面怎么做？补位轮 effort 837 缺位如何恢复？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 39 轮 = effort #837 / spec 837 / impl 591）：`RateLimitKeyHotspot`——键封顶 128+溢出桶、requests/amount 毫账累计+lastSeen、top 降序典序破平；键拼装归调用方；backend SPI 零变更。补位注记：R38 跳号缺位即时填补（effort 连续性恢复）。
