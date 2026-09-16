---
id: T3180
title: 版本要求判定的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3181]
created: 2026-09-17
---

## Question

VersionRequirement 合同（六算子/0.x 特例/边界/prerelease/畸形）怎么钉住？（spec 2039 / effort #2039 / R40）

## Resolution

**九用例全绿**（首版 lastVersion 隐式状态 hack 重写为解析一次纯比较
后 9/9）：CARET 稳定主（1.9.9 过 2.0.0 拒） / ^0.2.3 锁次 / ^0.0.3
锁补丁 / TILDE 次锁 / GTE 含 GT 不含 / EXACT+ANY / prerelease 恰界
低于+高于界认+EXACT 不认预发 / 短版本补 0（1.2=1.2.0、1=1.0.0） /
畸形五型（null/空/非数字段 ×2）fail-fast。
