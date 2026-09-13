# 772 — 金丝雀生命周期计数读面

**What to build:** CanaryGuardHook planted/leaked/variantBlocked 三计数 + 嵌套 CanaryStats + stats() + 双轨测试（复用既有 shim）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数（播撒幂等不重复计 / 泄漏 / 变体拦截）
- [x] CanaryStats 嵌套 record + stats()
- [x] CanaryGuardStatsTest（播撒幂等/泄漏/变体/无辜不计）
- [x] spec 1019 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='CanaryGuardStatsTest,InjectionDefenseUnitTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
