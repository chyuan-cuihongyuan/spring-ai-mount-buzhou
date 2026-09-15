---
id: T2878
title: 轮换重叠窗的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2877]
created: 2026-09-16
---

## Question]

重叠窗在三态/零宽/普查/畸形四面下正确吗？（spec 1838 / effort #1838 / R39）

## Resolution

**RotationOverlapWindowTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=RotationOverlapWindowTest）：三态+边界含（lag 2 为 GRACE、3 为
EXPIRED）；零宽窗硬切换；普查 2/2/1+expiredRatio 0.2+null 哨兵；负 epoch/
负宽/未来代/null 凭据 fail-fast。

