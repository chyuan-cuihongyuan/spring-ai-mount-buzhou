---
id: T2619
title: 会话迁移结果普查的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

MigrationOutcomeStats 的形状怎么裁决？（spec 1709 / effort #1709 / R10）（spec 1709 验收/裁决）

## Resolution

实例面 EnumMap+AtomicLong 四桶（MIGRATED/SKIPPED_CURRENT/SKIPPED_EMPTY/FAILED）+census()→MigrationCensus+attemptSuccessRatio（分母 migrated+failed，无尝试 −1）+resetForTest——Kafka 再均衡过程普查，与 spec 825 数据对账互补。
