# 842 — HITL 认证决策分布

> 来源：H 会话第 43 轮 = effort #842 / [T1185](../../.wayfinder/tickets/T1185-auth-decision-stats.md) / [T1186](../../.wayfinder/tickets/T1186-auth-decision-stats-verify.md) / impl 595。
> 借鉴：Keycloak required-actions 决策观测（扩散）。

## Problem

HITL 审批只有 approve/revoke 执行面：拒绝多还是凭据管理问题（过期/重复消费/未知凭证）多——决策分布缺位。

## Solution

`AuthDecisionStats`（guard.hook，纯记账）：五态闭集（GRANTED/DENIED/EXPIRED/CONSUMED/UNKNOWN）计数+占比降序快照；null 忽略；喂点=装配侧。

## 兼容性

纯新增；GuardAuthApi 零变更。

## 诚实边界

喂点手动；五态语义归 API；内存有界。
