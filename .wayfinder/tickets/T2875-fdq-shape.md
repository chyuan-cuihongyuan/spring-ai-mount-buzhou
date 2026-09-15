---
id: T2875
title: 失败域配额的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

预算的「域隔离」与「全局兜底」怎么两全？（spec 1837 / effort #1837 / R38）

## Resolution

**k8s failure-domain/供应链分域备货思想纯裁决 `FailureDomainQuota`
（core/budget）**：admit 三态 FROM_BUCKET（域桶先花）/BORROW_RESERVE
（桶满借全局保留——单域失败不连坐他域的兜底）/DENY（皆尽拒）+
census 域普查（atCap/borrowing/tightest 最紧域并列取首/
reserveUtilization -1 哨兵）。纯裁决零记账。

