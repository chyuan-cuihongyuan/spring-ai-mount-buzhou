# impl-105 — skill_search 检索工具

**What to build:** 目录截断外的技能可被模型运行时检索发现。

**Blocked by:** None

**Status:** done

- [x] SkillSearchTool（子串/不分大小写/上限 20/空结果指引）
- [x] SkillRegistry.listAllFor（不截断全集；DefaultSkillRegistry 覆写）
- [x] SkillModule 自动注册；绑定可见性过滤
- [x] 测试：截断外可检索/大小写/绑定裁剪/参数容错——skills 67/67 绿；spec 37 §A

## Done

commit：见 git log（impl-105）。
