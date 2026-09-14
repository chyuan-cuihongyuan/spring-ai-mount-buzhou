---
id: T2425
title: R38 慢调用 yml 装配的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2424
created: 2026-09-15
---

## Question

N 会话第 38 轮：慢调用配置挂顶层还是 Circuit 组？

## Resolution

选 **Circuit 组**（语义归位）。顶层 record 已 18 参——慢调用是熔断维度的
一部分，随组配置随组校验；rate 缺省 0.5 与失败率阈同档（两个保护维度
默认一致性）。R29 的链式注入保留（编程面），Module 装配读组传导。
