# 475 — 归档/还原每会话互斥

**What to build:** SessionArchiver 条目锁互斥 archive/restore（同会话串行、跨会话无涉）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 条目锁 + 两方法包装
- [x] 3 用例绿（含丢失窗关闭断言）+ core 全模块零回归
- [x] spec 622 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `fix(core)` 提交。
