# 847 — SkillAdmin×Search 可见性联动组合测试轮

**What to build:** SkillAdminSearchComboTest——发布→命中、下架→消失联动 + 双读面守恒。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SkillAdminSearchComboTest（联动/双守恒/归零三测）
- [x] spec 1095 + README 行

## Done

验证：`mvn -pl buzhou-skills -am test -Dtest='SkillAdminSearchComboTest'` 全绿。commit 见本轮 `test(skills)` 提交。
