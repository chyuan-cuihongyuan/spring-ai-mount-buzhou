# 535 — store SPI 契约套件接入示例

**What to build:** H2StoresContractTest 增契约方法——JdbcSessionStateStore 过 SessionStateStoreContract 九项检查（H2 无 Docker CI 口径）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 契约方法接入 + 全绿
- [x] spec 732 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-store-jdbc -am test` 绿。commit 见本轮 `test(store-jdbc)` 提交。
