---
id: T1015
title: 契约套件接入 store-jdbc 的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

SessionStateStoreContract（spec 705）需一次真实外部存储接入示范；store-jdbc 已有 H2 契约测试（AbstractBuzhouStoresContractTest）——接入点怎么选、与既有基类关系如何？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 33 轮 = effort #732 / spec 732 / impl 535）：H2StoresContractTest 增单测方法——SessionStateStoreContract.verify(stores().sessionStateStore()) 全绿断言。关系澄清：既有基类测 BuzhouStores 组装语义；G 契约测 SessionStateStore SPI 细节语义（CAS 消费一次/null-expect 首写/前缀过滤/幂等清场）——互补层。H2 内存库无 Docker 依赖——CI 直接跑（真实 SQL 路径）。
