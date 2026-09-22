---
id: T2981
title: 阻塞期审计的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

「算得慢还是等得久」的时间构成分诊怎么算？（spec 1890 / effort #1890 / R91）

## Resolution`

**Oracle ASH 会话状态语义纯计算 `WaitProfileAudit`（core/metrics）**：
profile（onCpu+lock+io=elapsed 账面守恒）+ dominantClass（CPU>I/O>
锁固定序取大并列确定性）+ blockingRatio（阻塞占比）。守恒破坏
fail-fast。落轮 grep 复核无占坑。
