---
id: T1041
title: dryRun×AlertGate 语义确认的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

dryRun 与 AlertGate 的交互动语义需显式确认。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 46 轮 = effort #746 / spec 746 / impl 548，测试域补验轮）：gate 在场吞通知时 dryRun.wouldFire 仍如实报告（推演不受门抑制——dry-run=规则引擎推演，gate=通知通道策略，正交）；evaluate 实弹照常被门吞（对照）。
