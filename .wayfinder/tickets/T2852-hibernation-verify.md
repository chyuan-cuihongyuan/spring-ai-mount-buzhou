---
id: T2852
title: 会话休眠分级的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2851]
created: 2026-09-16
---

## Question]

分级在档位/画像/普查/畸形四面下正确吗？（spec 1825 / effort #1825 / R26）

## Resolution

**SessionHibernationPolicyTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=SessionHibernationPolicyTest）：三档+双边界；画像单调（税 0/50/2000
与足迹 1.0/0.5/0.1）；普查混合节省率 0.46（容差断言——浮点直等假红一次
修正）/全活 0/全冷 0.9/空 -1；负闲置/倒挂/null fail-fast。

