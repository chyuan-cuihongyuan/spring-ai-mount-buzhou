# 844 — 摘要降级原因分布

> 来源：H 会话第 45 轮 = effort #844 / [T1189](../../.wayfinder/tickets/T1189-summary-degrade-reasons.md) / [T1190](../../.wayfinder/tickets/T1190-summary-degrade-reasons-verify.md) / impl 597。
> 借鉴：Envoy degraded 健康语义（扩散轮）。

## Problem

摘要降级管线（SummaryDegrader）执行降级但不留原因分布：超限截断为主还是生成失败为主——降级行为画像缺位。

## Solution

`SummaryDegradeReasons`（memory，纯记账）：五态闭集（OVER_LIMIT/GENERATION_FAILED/EMPTY_CONTENT/POLICY_FORCED/UNKNOWN）计数+占比降序快照；null 忽略；喂点=降级管线装配侧。

## 兼容性

纯新增；SummaryDegrader 零变更。

## 诚实边界

喂点手动；语义归调用方；内存有界。
