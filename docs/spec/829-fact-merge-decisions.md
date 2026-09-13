# 829 — 事实合并决策分布

> 来源：H 会话第 30 轮 = effort #829 / [T1159](../../.wayfinder/tickets/T1159-fact-merge-decisions.md) / [T1160](../../.wayfinder/tickets/T1160-fact-merge-decisions-verify.md) / impl 582。
> 借鉴：mem0 冲突解决统计（G 会话 R35 思想族扩散）。

## Problem

摘要对账每段做「新建/沿用/替换」三分决策（SummaryFactReconciler）：九段的决策偏好（哪段常被改写、替换率多高）无统计面——LLM 对账行为是黑盒。

## Solution

`FactMergeDecisionDistribution`（memory，纯记账）：

- **三分决策**：CREATED / KEPT / SUPERSEDED；9 段闭集×3=27 格天然有界。
- **报告**：total/created/kept/superseded/supersededRatio + 段行（声明序，只含触碰段）。
- **喂点解耦**：对账管线装配侧接（EVENT_RECONCILED 消费者）——reconcile 行为零变更。

## 兼容性

纯新增；SummaryFactReconciler 零变更。

## 诚实边界

计数不评价（改写风格好坏归业务判断）；跨会话聚合调用方分桶；进程内存有界。
