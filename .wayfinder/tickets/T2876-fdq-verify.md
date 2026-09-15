---
id: T2876
title: 失败域配额的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2875]
created: 2026-09-16
---

## Question]

三态准入与普查在四读数/哨兵/畸形下正确吗？（spec 1837 / effort #1837 / R38）

## Resolution

**FailureDomainQuotaTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=FailureDomainQuotaTest）：三态（5/10 桶内→10/10 借→12/10+5/5 拒）；
普查 atCap=1/borrowing=1/tightest=b/保留 25%+并列取首；空表/null 哨兵；
负计数/空白域 fail-fast。首跑红为并行会话编辑窗口（收尾后绿）。

