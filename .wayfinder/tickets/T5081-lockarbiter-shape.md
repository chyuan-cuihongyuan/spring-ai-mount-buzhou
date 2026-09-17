---
id: T5081
title: Q 会话 R41 时间戳锁的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

资源互锁怎么事前预防而非事后检测？（spec 3040 / effort #3040 / R41）

## Resolution

**TimestampLockArbiter（core/concurrent）**：Rosenkrantz 1978——
wait-die 老等少死 / wound-wait 老伤少等双模式，年长者
(timestamp,txnId) 字典序特权，等待图恒无环（死锁预防）；纯仲裁
无阻塞（等待/重启动作归调用方）；woundedTxn 指认受害者。与
TarjanSccFinder 成对（事前预防 vs 事后指认）。
