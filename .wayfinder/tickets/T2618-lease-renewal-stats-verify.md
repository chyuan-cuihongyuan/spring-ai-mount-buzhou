---
id: T2618
title: 租约续期抖动读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2617
created: 2026-09-15
---

## Question

LeaseRenewalStats 怎么验证？（spec 1708 验收/裁决）

## Resolution

LeaseRenewalStatsTest：空/单哨兵/恒定 cv≈0 且 skew=0/抖动 cv>0.3 且 skew=8s/负值忽略。
