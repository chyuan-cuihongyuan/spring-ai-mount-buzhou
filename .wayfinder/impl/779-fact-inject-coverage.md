# 779 — 事实注入覆盖读面

**What to build:** FactAttachmentRenderer renders/factsInjected/factsOmitted 三计数 + 嵌套 FactInjectStats + stats() + 双重载收敛与覆盖测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数埋点（注入/省略/非空渲染）
- [x] FactInjectStats 嵌套 record + stats()
- [x] 两参 render 委托收敛（输出恒等）
- [x] FactInjectCoverageTest（全注入/省略/空仓不计）
- [x] spec 1026 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='FactInjectCoverageTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
