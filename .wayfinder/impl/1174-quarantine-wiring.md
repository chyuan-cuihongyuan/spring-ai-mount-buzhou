# 1174 — 会话隔离检疫装配

**What to build:** SessionQuarantine+Hook 的 opt-in 装配 bean + hook 行为测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] BuzhouCoreAutoConfiguration：buzhou.quarantine.* 装配（默认关）
- [x] SessionQuarantineHookTest 两断言 + 既有 6 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest=SessionQuarantine*Test`。
