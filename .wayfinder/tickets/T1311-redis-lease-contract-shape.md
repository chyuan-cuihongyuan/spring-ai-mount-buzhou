---
id: T1311
title: 租约契约接入 Redis store 的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 31 轮：spec 922 租约契约的 Redis 实现接入（jedismock hermetic 基建）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 31 轮 = effort #930 / spec 930 / impl 683）：接入成立。落点 store-redis 测试域 `RedisLeaseContractTest`（jedismock hermetic 基建——RedisStoresContractTest 同款，无 Docker 依赖）。**契约首跑即抓到真实语义缺陷**：`RedisSessionLeaseStore.tryAcquire` 的 ACQUIRE_SCRIPT「EXISTS 即拒」缺少 SPI 幂等重入语义（SessionLeaseStore/LeaderElector javadoc：「已是持有人则续期并返回原 token」）——修复=ACQUIRE_SCRIPT 加同 owner 重入分支（PEXPIRE 续期返回原 token，异 owner 仍拒）。九项全过；既有 RedisStoresContractTest 16 用例零回归。
