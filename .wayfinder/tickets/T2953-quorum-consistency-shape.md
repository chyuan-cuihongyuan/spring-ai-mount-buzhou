---
id: T2953
title: 法定人数一致性的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-22
---

## Question)

多副本 R/W 配置的一致性等级与可容忍故障怎么算？（spec 1876 / effort #1876 / R77）

## Resolution`

**Dynamo/Cassandra quorum 交集语义纯计算 `QuorumConsistency`
（core/transaction）**：strongConsistency（R+W>N——读集写集必交，
读到最新写有保证）+ overlapCount（R+W−N 负值钳 0——保证公共副本
数）+ tolerableWrite/ReadFailures（N−W / N−R）+ consistentAvailability
（min 双余量——一致性同时保住的最大故障数）。N≥1、1≤R≤N、1≤W≤N
fail-fast。纯计算零状态，配置前有账。
