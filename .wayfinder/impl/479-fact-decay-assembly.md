# 479 — 事实衰减装配扩散

**What to build:** 衰减类移驻 core.internal.memory + GuardModule.Builder.factDecay + yml 装配。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 移驻（类+测试整体迁移，包撤销）
- [x] Builder + 构造包装 + yml 键
- [x] 装配 2 用例 + 回归 5/5 + 三模块全量零回归
- [x] spec 626 + README 行

## Done

验证：`mvn -pl buzhou-core,buzhou-guard,buzhou-memory -am test` 绿。commit 见本轮 `feat(guard)` 提交。
