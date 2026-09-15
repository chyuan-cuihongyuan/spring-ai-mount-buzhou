---
id: T2867
title: 续读令牌的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

游标续读怎么知道「还是那一版数据」？（spec 1833 / effort #1833 / R34）

## Resolution

**continuation token/ETag 思想 `ResumeTokenCodec`（core/session）**：
ResumeToken(fingerprint, offset) 契约构造 + encode/decode 回路（指纹@偏移，
按最后分隔符切，指纹侧容忍内含 @）+ check 三态 VALID/STALE_DATA/
OUT_OF_RANGE（**指纹先行**——换代优先于越界；offset==max 为读到尾
VALID）。纯裁决零读取。

