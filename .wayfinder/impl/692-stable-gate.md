# 692 — k 次防抖门

**What to build:** EvalGate.enforceStable（k 次 enforce 循环 + 全过裁决 + 历史容量校验）+ 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] enforceStable
- [x] StableGateTest（全过/任一失败/历史条数/k 越界）
- [x] spec 943 + README 行（欠账累计 926–943）

## Done

验证：`mvn -pl buzhou-core test -Dtest=StableGateTest` 全绿。commit 见本轮 `feat(core)` 提交。
