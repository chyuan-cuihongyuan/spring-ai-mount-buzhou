# 483 — 混合排序装配

**What to build:** SkillRanker 接口抽取 + hybrid yml 键 + 模块装配（目录+检索共享）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 接口 + 双实现 + 参数面替换
- [x] Builder/fromYml hybrid 键 + fail-fast
- [x] 3 用例绿 + skills 全模块零回归 + 快照再生
- [x] spec 630 + README 行

## Done

验证：`mvn -pl buzhou-skills -am test` 绿。commit 见本轮 `feat(skills)` 提交。
