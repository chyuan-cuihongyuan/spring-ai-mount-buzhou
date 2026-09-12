# 508 — store SPI 契约校验套件

**What to build:** SessionStateStoreContract.verify(store)——九项 SPI 语义契约检查 + Report（逐项通过/失败明细）+ `__contract__` 会话自清理；真实实现全绿、走样实现逐项红。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionStateStoreContract（九项检查 + Report/Check record + 自清理）
- [x] 真实 InMemory 实现全绿用例
- [x] 三类走样实现逐项红 + 失败名可读
- [x] 零残留 + Report 不可变
- [x] spec 705 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
