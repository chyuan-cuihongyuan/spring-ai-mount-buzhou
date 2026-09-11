# 469 — 技能目录清单指纹

**What to build:** SkillCatalogFingerprint（sha256(description|allowedTools) 表 + 摘要 + diff 三分类）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 指纹原语 + diff
- [x] 4 用例绿
- [x] spec 616 + README 行

## Done

验证：`mvn -pl buzhou-skills -am test -Dtest=SkillCatalogFingerprintTest` 绿（4/4）。commit 见本轮 `feat(skills)` 提交。
