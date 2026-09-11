# 470 — 技能目录漂移看门狗

**What to build:** SkillCatalogDriftWatcher（616 指纹的 201 式接线）+ 616 diff 方向对齐 175 修正。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 看门狗（基线/事件/计数/推进）
- [x] 616 diff 方向修正（added=参数侧，基线.diff(现)=时间正向）
- [x] 2+4 用例绿，skills 全模块零回归
- [x] spec 617 + README 行

## Done

验证：`mvn -pl buzhou-skills -am test` 绿。commit 见本轮 `feat(skills)` 提交。
