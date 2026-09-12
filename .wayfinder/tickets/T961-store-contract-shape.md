---
id: T961
title: store SPI 契约校验套件的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

SessionStateStore SPI 有 CAS/前缀扫描/会话级删除等精细语义（HITL 一次性放行、日翻越首写竞态钉住都压在其上）——第三方 store 实现若语义走样（如 CAS 非原子、scanByPrefix 漏前缀），上层机制静默劣化。Pact 式消费者驱动契约测试怎么给第三方复用？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 6 轮 = effort #705 / spec 705 / impl 508）：`SessionStateStoreContract`（core/spi，**主源码无 JUnit 依赖**）——静态 `verify(SessionStateStore)` 跑九项有序契约检查（put/get 往返、getAll 全量、delete 移除、未知会话空、deleteIfValueMatches 消费一次 CAS、CAS null-expect 首写、CAS match 覆写、deleteSession 幂等清场、scanByPrefix 前缀过滤），返回 `Report`（List<Check(name,passed,detail)> + passed() + 失败名清单）。检查用 `__contract__` 前缀会话 + try/finally deleteSession 自清理（对真实存储零残留）。抽象基类 + 测试运行器绑定不做（主源码不进 JUnit；第三方在自家测试里一行调 verify 即可，或启动诊断直接消费 Report）。借鉴 Pact consumer contract testing（契约即可执行验证物）+ Testcontainers SPI 契约基类惯例。
