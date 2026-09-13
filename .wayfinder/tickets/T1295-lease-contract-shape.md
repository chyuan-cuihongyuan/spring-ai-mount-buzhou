---
id: T1295
title: SessionLeaseStore 契约校验套件的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 23 轮：spec 705/743/744 先例之后，核心 SPI 的契约校验套件还缺 SessionLeaseStore（租约语义最复杂：acquire/renew/release/steal/inspect/deleteSession 七方法）——同构扩散是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 23 轮 = effort #922 / spec 922 / impl 675）：扩散成立。落点 `SessionLeaseStoreContract`（spi 包，SessionStateStoreContract 同款范式——静态 verify 逐项收集不抛、主源码零 JUnit 依赖）：九项语义检查——①tryAcquire 空闲会话成功且 fence 正 ②重复 acquire 同 owner 幂等 ③异 owner acquire 被拒 ④renew 持有人续期成功 ⑤renew 非持有人失败 ⑥release 后可再 acquire ⑦steal 抢占成功且 fence 递增 ⑧inspect 反映租约状态 ⑨deleteSession 幂等清理。配套 `LeaseContractAccessTest`（H2/内存实现接入示例——spec 732 复用面）在 core 测试域跑 InMemorySessionLeaseStore 过九项。
