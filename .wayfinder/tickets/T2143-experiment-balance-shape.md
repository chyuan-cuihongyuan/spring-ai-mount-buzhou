---
id: T2143
title: 实验分桶均衡审计（ExperimentBalanceAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 22 轮：实验分配均衡审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：ExperimentBucketer 只做分配无均衡自证面——A/A 检查轴全空。

形状裁决：ExperimentBalanceAudit 纯函数——两段式 forBuckets(声明桶集合)→withAssignments（零桶计入不能装不存在）+BucketShare(share/deviation)+BalanceReport(maxDeviation≤BALANCE_TOLERANCE=5pp 含端点+1e-9 双比较卫生)+无样本 -1 哨兵 balanced=false 不冒充；buckets 降序典序。

Out of scope：卡方检验；holdout 审计；跨实验交叉。
