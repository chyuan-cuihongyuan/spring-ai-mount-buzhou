# 546 — MessageStore SPI 契约校验套件

**What to build:** MessageStoreContract.verify(store)——四项语义契约（append/load 保序、未知会话空读、多次追加保序、deleteSession 幂等）+ 探针自清理。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] MessageStoreContract（四项检查 + Report/Check + 自清理）
- [x] InMemory 全绿 + 走样红 + 零残留用例
- [x] spec 743 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
