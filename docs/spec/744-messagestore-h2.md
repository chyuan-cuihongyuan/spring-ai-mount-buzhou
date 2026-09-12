# 744 — MessageStore 契约接入 H2

> 来源：G 会话第 45 轮 = effort #745（spec 743 复用面）/ [T1039](../../.wayfinder/tickets/T1039-messagestore-h2-shape.md) / [T1040](../../.wayfinder/tickets/T1040-messagestore-h2-verify.md) / impl 547。

## 背景

MessageStoreContract（spec 743）需真实 SQL 存储接入示范——与 spec 732 的 H2 接入同模式。

## 目标

- H2StoresContractTest 增 `messageStoreSatisfiesGSessionContract`：JdbcMessageStore 过四项契约（时序保序/幂等清场在 SQL 存储上全绿）。

## 兼容性

纯测试域增量。
