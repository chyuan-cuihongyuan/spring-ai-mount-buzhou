# 929 — 租约契约接入 H2/JDBC store

> 来源：I 会话第 30 轮 = effort #927 续（[T1309](../../.wayfinder/tickets/T1309-h2-lease-contract-shape.md) / [T1310](../../.wayfinder/tickets/T1310-h2-lease-contract-verify.md) / impl 682）。spec 732/744 契约接入先例（H2 无 Docker CI 口径）。

## 背景

spec 922 的 `SessionLeaseStoreContract` 只有内存实现接入示例。`JdbcSessionLeaseStore` 是生产级实现（真实 SQL fence 语义）——契约接入即「多实例安全」自证落地。

## 目标

- store-jdbc 测试域 `H2LeaseContractTest`：H2 内存库（UUID 库名 + `DB_CLOSE_DELAY=-1` 既有测试基建）经 `JdbcBuzhouStores.createWithRecovery` 构造 `JdbcSessionLeaseStore` → `SessionLeaseStoreContract.verify` 九项全过断言（失败清单可读）。

## 兼容性

纯测试轮；零生产代码变更（契约抓出缺陷按先例修复并补记）。
