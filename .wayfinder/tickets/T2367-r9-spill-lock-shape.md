---
id: T2367
title: R9 DiskSpillStore 锁迁移的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2366
created: 2026-09-15
---

## Question

N 会话第 9 轮：spill 写路径 pinning 修复形状？

## Resolution

选 **ReentrantLock 迁移**（spec 1607 同款先例延续）。store/usage 两方法共一把锁：
写路径低频（溢出才发生），拆锁无收益；「一次调用一次 spill」的存在性互斥与配额
计量互斥原样保留。
