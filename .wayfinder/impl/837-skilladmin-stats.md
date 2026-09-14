# 837 — Skill 管理操作读面

**What to build:** SkillAdminApi 静态五计数（creates/updates/publishes/disables/deletes）+ 嵌套 SkillAdminStats + stats()/resetForTest() + 五操作/校验不入桶/归零测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数落点（五操作成功返回处；校验异常不入桶）
- [x] SkillAdminStats 嵌套 record + stats() + resetForTest()
- [x] SkillAdminStatsTest（五操作/校验不入桶/归零三测）
- [x] spec 1085 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-skills -am test -Dtest='SkillAdminStatsTest'` 全绿 + 既有 SkillAdminApi 回归绿。commit 见本轮 `feat(skills)` 提交。
