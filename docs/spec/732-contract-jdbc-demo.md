# 732 — store SPI 契约套件接入示例（H2 实存储）

> 来源：G 会话第 33 轮 = effort #732（spec 705 复用面）/ [T1015](../../.wayfinder/tickets/T1015-contract-jdbc-shape.md) / [T1016](../../.wayfinder/tickets/T1016-contract-jdbc-verify.md) / impl 535。

## 背景

SessionStateStoreContract（spec 705）只对 InMemory 实现验证过——「第三方 store 实现一行自证」的价值主张需要一次**真实外部存储**接入示范。store-jdbc 已有 H2 契约测试（AbstractBuzhouStoresContractTest，无 Docker 依赖 CI 可跑）。

## 目标

- H2StoresContractTest 增 `sessionStateStoreSatisfiesGSessionContract`：对 JdbcSessionStateStore 跑 SessionStateStoreContract.verify——九项 CAS/前缀扫描/清场语义在真实 SQL 存储上全绿。
- 与既有 AbstractBuzhouStoresContractTest 关系：基类测 BuzhouStores 组装面；本契约测 SessionStateStore SPI 语义细节（CAS 消费一次/null-expect 首写/前缀过滤）——互补不重复。

## 测试

H2 实存储 verify 全绿 9/9（CI 口径——无 Docker 依赖）。

## 兼容性

纯测试域增量。
