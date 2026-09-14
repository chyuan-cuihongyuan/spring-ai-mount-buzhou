---
id: T2311
title: 类级 Javadoc 全仓收口（五-1 终轮）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2309
created: 2026-09-15
---

## Question

M 会话第 34 轮：design-incompleteness 五-1 的最后尾巴（resilience 1/guard 1/store-jdbc 5/store-redis 1）与 core 六包外 30 个的处置？

## Resolution

**用户常设授权 AFK（可推翻）**

① 8 个补齐（AutoConfiguration×3 + JdbcStore 五 SPI 实现——SPI 实现类是契约消费方可见面）；② core 六包外 30 个裁定不补：CoreApiJavadocCoverageTest 门辖界=语义 API 承诺面（session/hook/exec/spi/observability/error），六包外是 internal 实现域（webhook/cleanup/experiment 等），类级 Javadoc 无语义版本承诺意义——过度补齐是噪音。五-1 全档闭环。
