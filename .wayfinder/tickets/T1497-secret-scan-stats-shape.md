---
id: T1497
title: 密钥扫描计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 24 轮：密钥扫描计数读面（Gitleaks findings 思想）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 24 轮 = effort #1023 / spec 1023 / impl 776）：缺口成立——SecretScanner（spec 400 双 hook 生产接线）scan/redact 全程零计数：扫描执行多少次、命中多少凭据、实际脱敏多少不可见——防线覆盖与命中水位（泄漏趋势）无据。落点 buzhou-guard secret 包：实例级 scanCalls/findings/redactions 三 AtomicLong——scan 入口计调用（空文本早退不计）、命中数按条累加；redact 实际替换时计 redactions（其内部 scan 亦计 scanCalls——口径入档）；幂等早返（已含占位符）不计。嵌套 record `SecretScanStats(scanCalls, findings, redactions)` + `stats()`。实例级；嵌套类型不动 API 快照；行为逐位不变。
