# 930 — 租约契约接入 Redis store

> 来源：I 会话第 31 轮 = effort #930（[T1311](../../.wayfinder/tickets/T1311-redis-lease-contract-shape.md) / [T1312](../../.wayfinder/tickets/T1312-redis-lease-contract-verify.md) / impl 683）。spec 922 契约扩散第三站（内存 → JDBC → Redis）。

## 背景

`RedisSessionLeaseStore` 的 ACQUIRE_SCRIPT 为「EXISTS 即拒」——同 owner 重入被拒，违反 SPI 幂等重入语义（`SessionLeaseStore`/`LeaderElector` javadoc：「已是持有人则续期并返回原 token」）。契约套件首跑即显形。

## 目标

- ACQUIRE_SCRIPT 加同 owner 重入分支：EXISTS 且 owner 匹配 → PEXPIRE 续期、返回原 fencingToken（幂等）；异 owner → 0（互斥不变）；空闲 → INCR 取新 token（不变）；
- `RedisLeaseContractTest`（jedismock hermetic 基建）九项全过断言；
- 既有 `RedisStoresContractTest` 全量零回归。

## 兼容性

行为修复：同 owner 重入从「拒」修正为「幂等续期」（SPI 语义归位）；异 owner 互斥、steal、release 语义不变。
