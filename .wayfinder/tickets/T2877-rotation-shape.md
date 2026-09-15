---
id: T2877
title: 轮换重叠窗的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

凭据轮换的蓝绿相接怎么判态？（spec 1838 / effort #1838 / R39）

## Resolution

**TLS 证书轮换/Vault grace 思想纯判态 `RotationOverlapWindow`
（core/crypto）**：validity 三态 CURRENT/GRACE（相差≤graceEpochs 含
边界——旧代在飞数据兼容）/EXPIRED；未来代 fail-fast（单调性违和不吞）；
census 普查（三态计数+expiredRatio 清扫进度 -1 哨兵）；零宽窗合法
（硬切换对照面）。纯判态零执行。

