---
id: T3176
title: 同步副本追踪器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3175]
created: 2026-09-17
---

## Question

InSyncTracker 合同（界语义/剔除回归/不回拨/收缩计数/畸形）怎么钉住？（spec 2037 / effort #2037 / R38）

## Resolution

**七用例一次全绿**（buzhou-core）：距追上 5s 同步、恰 10s 界外掉 /
滞后 a 剔除收缩计 1、追平回归不增缩 / 新注册初始同步从未追上即掉 /
迟到旧追平不回拨 / 未注册 false / 双掉出累计收缩 2 / 畸形七型（阈值
0、null/空成员、重复注册、未注册追平、负时刻 ×2）fail-fast。
