# 844 — Spotlight×Moderation 顺序协作组合测试轮

**What to build:** SpotlightModerationComboTest——同 ctx 双 hook 顺序协作 + 双读面计数一致 + 守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SpotlightModerationComboTest（顺序协作/计数一致/守恒三测）
- [x] spec 1092 + README 行

## Done

验证：`mvn -pl buzhou-guard -am test -Dtest='SpotlightModerationComboTest'` 全绿。commit 见本轮 `test(guard)` 提交。
